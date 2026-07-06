package com.sresthaa.admin.totp;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.dto.TotpEnrollmentOptions;
import com.sresthaa.admin.dto.TotpStatus;
import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.model.TotpBackupCode;
import com.sresthaa.admin.model.TotpCredential;
import com.sresthaa.admin.repository.TotpBackupCodeRepository;
import com.sresthaa.admin.repository.TotpCredentialRepository;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

@Service
public class TotpService {

	private static final int BACKUP_CODE_COUNT = 8;

	private final TotpCredentialRepository totpCredentialRepository;
	private final TotpBackupCodeRepository totpBackupCodeRepository;
	private final PasswordEncoder passwordEncoder;
	private final String issuer;

	private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
	private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(),
			new SystemTimeProvider());
	private final QrDataFactory qrDataFactory = new QrDataFactory(HashingAlgorithm.SHA1, 6, 30);
	private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
	private final RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();

	public TotpService(TotpCredentialRepository totpCredentialRepository,
			TotpBackupCodeRepository totpBackupCodeRepository, PasswordEncoder passwordEncoder,
			@Value("${webauthn.relying-party-name}") String issuer) {
		this.totpCredentialRepository = totpCredentialRepository;
		this.totpBackupCodeRepository = totpBackupCodeRepository;
		this.passwordEncoder = passwordEncoder;
		this.issuer = issuer;
	}

	public boolean hasTotp(AdminAccount account) {
		return totpCredentialRepository.findByAdminAccountId(account.getId()).map(TotpCredential::isEnabled)
				.orElse(false);
	}

	public TotpStatus status(AdminAccount account) {
		return totpCredentialRepository.findByAdminAccountId(account.getId())
				.map(credential -> new TotpStatus(credential.isEnabled(), credential.isEnabled()
						? totpBackupCodeRepository.findByTotpCredentialIdAndUsedFalse(credential.getId()).size()
						: 0))
				.orElse(new TotpStatus(false, 0));
	}

	public TotpEnrollmentOptions beginEnrollment(AdminAccount account) {
		TotpCredential credential = totpCredentialRepository.findByAdminAccountId(account.getId()).orElse(null);
		if (credential != null && credential.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Two-factor authentication is already enabled - disable it first to re-enroll");
		}

		String secret = secretGenerator.generate();
		if (credential == null) {
			credential = new TotpCredential(account, secret);
		} else {
			credential.setSecret(secret);
		}
		totpCredentialRepository.save(credential);

		QrData qrData = qrDataFactory.newBuilder().label(account.getUsername()).issuer(issuer).secret(secret)
				.build();
		String qrCodeImagePng;
		try {
			qrCodeImagePng = Base64.getEncoder().encodeToString(qrGenerator.generate(qrData));
		} catch (QrGenerationException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate QR code", e);
		}

		return new TotpEnrollmentOptions(secret, qrData.getUri(), qrCodeImagePng);
	}

	public List<String> confirmEnrollment(AdminAccount account, String code) {
		TotpCredential credential = totpCredentialRepository.findByAdminAccountId(account.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Start enrollment before confirming a code"));
		if (credential.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Two-factor authentication is already enabled");
		}
		if (!codeVerifier.isValidCode(credential.getSecret(), code)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
		}

		credential.setEnabled(true);
		totpCredentialRepository.save(credential);

		return regenerateBackupCodes(credential);
	}

	public List<String> regenerateBackupCodes(AdminAccount account) {
		TotpCredential credential = totpCredentialRepository.findByAdminAccountId(account.getId())
				.filter(TotpCredential::isEnabled)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
						"Two-factor authentication is not enabled"));
		return regenerateBackupCodes(credential);
	}

	private List<String> regenerateBackupCodes(TotpCredential credential) {
		totpBackupCodeRepository.deleteAll(totpBackupCodeRepository.findByTotpCredentialId(credential.getId()));

		String[] rawCodes = recoveryCodeGenerator.generateCodes(BACKUP_CODE_COUNT);
		for (String rawCode : rawCodes) {
			totpBackupCodeRepository.save(new TotpBackupCode(credential, passwordEncoder.encode(rawCode)));
		}
		return List.of(rawCodes);
	}

	public void verifyLoginCode(AdminAccount account, String code) {
		TotpCredential credential = enabledCredentialOrThrow(account);

		if (codeVerifier.isValidCode(credential.getSecret(), code)) {
			return;
		}

		List<TotpBackupCode> unusedCodes = totpBackupCodeRepository
				.findByTotpCredentialIdAndUsedFalse(credential.getId());
		TotpBackupCode matchedBackupCode = unusedCodes.stream()
				.filter(backupCode -> passwordEncoder.matches(code, backupCode.getCodeHash())).findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code"));

		matchedBackupCode.markUsed(Instant.now());
		totpBackupCodeRepository.save(matchedBackupCode);
	}

	public void disable(AdminAccount account) {
		TotpCredential credential = totpCredentialRepository.findByAdminAccountId(account.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Two-factor authentication is not set up"));

		totpBackupCodeRepository.deleteAll(totpBackupCodeRepository.findByTotpCredentialId(credential.getId()));
		totpCredentialRepository.delete(credential);
	}

	private TotpCredential enabledCredentialOrThrow(AdminAccount account) {
		return totpCredentialRepository.findByAdminAccountId(account.getId()).filter(TotpCredential::isEnabled)
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Two-factor authentication is not enabled"));
	}
}
