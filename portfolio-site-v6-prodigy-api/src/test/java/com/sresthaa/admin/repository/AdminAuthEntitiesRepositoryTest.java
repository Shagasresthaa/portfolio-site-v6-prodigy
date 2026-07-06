package com.sresthaa.admin.repository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.model.TotpBackupCode;
import com.sresthaa.admin.model.TotpCredential;
import com.sresthaa.admin.model.WebAuthnCredential;

@SpringBootTest
@Transactional
class AdminAuthEntitiesRepositoryTest {

	@Autowired
	private AdminAccountRepository adminAccountRepository;

	@Autowired
	private WebAuthnCredentialRepository webAuthnCredentialRepository;

	@Autowired
	private TotpCredentialRepository totpCredentialRepository;

	@Autowired
	private TotpBackupCodeRepository totpBackupCodeRepository;

	@Test
	void persistsAndReloadsAdminAccount() {
		AdminAccount saved = adminAccountRepository.save(new AdminAccount("admin", "hashed-password"));

		AdminAccount found = adminAccountRepository.findByUsername("admin").orElseThrow();

		assertEquals(saved.getId(), found.getId());
		assertNotNull(found.getCreatedAt());
		assertNotNull(found.getUpdatedAt());
	}

	@Test
	void persistsWebAuthnCredentialWithBinaryFields() {
		AdminAccount account = adminAccountRepository.save(new AdminAccount("admin-webauthn", "hashed-password"));
		byte[] credentialId = { 1, 2, 3, 4, 5 };
		byte[] publicKey = { 10, 20, 30 };

		webAuthnCredentialRepository
				.save(new WebAuthnCredential(account, credentialId, publicKey, 0L, "usb,nfc", "YubiKey 5C"));

		WebAuthnCredential found = webAuthnCredentialRepository.findByAdminAccountId(account.getId()).get(0);

		assertArrayEquals(credentialId, found.getCredentialId());
		assertArrayEquals(publicKey, found.getPublicKeyCose());
		assertEquals("YubiKey 5C", found.getLabel());
	}

	@Test
	void encryptsAndDecryptsTotpSecretRoundTrip() {
		AdminAccount account = adminAccountRepository.save(new AdminAccount("admin-totp", "hashed-password"));

		totpCredentialRepository.save(new TotpCredential(account, "JBSWY3DPEHPK3PXP"));
		totpCredentialRepository.flush();

		TotpCredential found = totpCredentialRepository.findByAdminAccountId(account.getId()).orElseThrow();

		assertEquals("JBSWY3DPEHPK3PXP", found.getSecret());
	}

	@Test
	void persistsBackupCodesLinkedToTotpCredential() {
		AdminAccount account = adminAccountRepository.save(new AdminAccount("admin-backup", "hashed-password"));
		TotpCredential totp = totpCredentialRepository.save(new TotpCredential(account, "JBSWY3DPEHPK3PXP"));

		TotpBackupCode code = totpBackupCodeRepository.save(new TotpBackupCode(totp, "hashed-code-value"));
		code.markUsed(Instant.now());
		totpBackupCodeRepository.save(code);

		TotpBackupCode found = totpBackupCodeRepository.findByTotpCredentialId(totp.getId()).get(0);

		assertTrue(found.isUsed());
		assertNotNull(found.getUsedAt());
	}
}
