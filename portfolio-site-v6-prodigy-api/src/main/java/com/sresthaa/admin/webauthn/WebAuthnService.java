package com.sresthaa.admin.webauthn;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.dto.WebAuthnCredentialSummary;
import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.model.WebAuthnCredential;
import com.sresthaa.admin.repository.WebAuthnCredentialRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;

@Service
public class WebAuthnService {

	private final WebAuthnCredentialRepository webAuthnCredentialRepository;
	private final WebAuthnChallengeStore challengeStore;
	private final ObjectConverter objectConverter = new ObjectConverter();
	private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);

	private final String relyingPartyId;
	private final String relyingPartyName;
	private final Origin origin;

	public WebAuthnService(WebAuthnCredentialRepository webAuthnCredentialRepository,
			WebAuthnChallengeStore challengeStore,
			@Value("${webauthn.relying-party-id}") String relyingPartyId,
			@Value("${webauthn.relying-party-name}") String relyingPartyName,
			@Value("${webauthn.origin}") String origin) {
		this.webAuthnCredentialRepository = webAuthnCredentialRepository;
		this.challengeStore = challengeStore;
		this.relyingPartyId = relyingPartyId;
		this.relyingPartyName = relyingPartyName;
		this.origin = new Origin(origin);
	}

	public boolean hasCredentials(AdminAccount account) {
		return !webAuthnCredentialRepository.findByAdminAccountId(account.getId()).isEmpty();
	}

	public List<WebAuthnCredentialSummary> listCredentials(AdminAccount account) {
		return webAuthnCredentialRepository.findByAdminAccountId(account.getId()).stream()
				.map(WebAuthnCredentialSummary::from)
				.toList();
	}

	public void deleteCredential(AdminAccount account, UUID credentialId) {
		WebAuthnCredential credential = webAuthnCredentialRepository.findById(credentialId)
				.filter(cred -> cred.getAdminAccount().getId().equals(account.getId()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such security key"));

		webAuthnCredentialRepository.delete(credential);
	}

	public String beginRegistration(AdminAccount account) {
		Challenge challenge = new DefaultChallenge();
		challengeStore.putRegistrationChallenge(account.getUsername(), challenge);

		List<PublicKeyCredentialDescriptor> excludeCredentials = webAuthnCredentialRepository
				.findByAdminAccountId(account.getId()).stream()
				.map(cred -> new PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY,
						cred.getCredentialId(), null))
				.toList();

		PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions(
				new PublicKeyCredentialRpEntity(relyingPartyId, relyingPartyName),
				new PublicKeyCredentialUserEntity(account.getId().toString().getBytes(StandardCharsets.UTF_8),
						account.getUsername(), account.getUsername()),
				challenge,
				List.of(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY,
						COSEAlgorithmIdentifier.ES256)),
				null,
				excludeCredentials,
				new AuthenticatorSelectionCriteria(null, true, UserVerificationRequirement.REQUIRED),
				AttestationConveyancePreference.NONE,
				null);

		return objectConverter.getJsonMapper().writeValueAsString(options);
	}

	public void verifyRegistration(AdminAccount account, String registrationResponseJSON, String label) {
		Challenge challenge = challengeStore.consumeRegistrationChallenge(account.getUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"No pending registration challenge - request options again"));

		ServerProperty serverProperty = ServerProperty.builder().origin(origin).rpId(relyingPartyId).challenge(challenge)
				.build();
		RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, null, true);

		RegistrationData registrationData;
		try {
			registrationData = webAuthnManager.verifyRegistrationResponseJSON(registrationResponseJSON,
					registrationParameters);
		} catch (RuntimeException e) {
			// Covers DataConversionException/VerificationException, but webauthn4j can also throw
			// a plain NPE on malformed/incomplete input - either way it's a bad request.
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration response", e);
		}

		AttestedCredentialData attestedCredentialData = registrationData.getAttestationObject().getAuthenticatorData()
				.getAttestedCredentialData();
		long signCount = registrationData.getAttestationObject().getAuthenticatorData().getSignCount();
		byte[] coseKeyBytes = objectConverter.getCborMapper().writeValueAsBytes(attestedCredentialData.getCOSEKey());

		webAuthnCredentialRepository.save(new WebAuthnCredential(
				account,
				attestedCredentialData.getAaguid().getBytes(),
				attestedCredentialData.getCredentialId(),
				coseKeyBytes,
				signCount,
				null,
				label));
	}

	public String beginAuthentication(AdminAccount account) {
		Challenge challenge = new DefaultChallenge();
		challengeStore.putAuthenticationChallenge(account.getUsername(), challenge);

		List<PublicKeyCredentialDescriptor> allowCredentials = webAuthnCredentialRepository
				.findByAdminAccountId(account.getId()).stream()
				.map(cred -> new PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY,
						cred.getCredentialId(), null))
				.toList();

		PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions(
				challenge,
				null,
				relyingPartyId,
				allowCredentials,
				UserVerificationRequirement.REQUIRED,
				null);

		return objectConverter.getJsonMapper().writeValueAsString(options);
	}

	public void verifyAuthentication(AdminAccount account, String authenticationResponseJSON) {
		Challenge challenge = challengeStore.consumeAuthenticationChallenge(account.getUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
						"No pending authentication challenge - password must be verified again"));

		AuthenticationData authenticationData;
		try {
			authenticationData = webAuthnManager.parseAuthenticationResponseJSON(authenticationResponseJSON);
		} catch (RuntimeException e) {
			// Covers DataConversionException, but webauthn4j can also throw a plain NPE on
			// malformed/incomplete input - either way it's a bad request, not a server error.
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authentication response", e);
		}

		WebAuthnCredential storedCredential = webAuthnCredentialRepository
				.findByCredentialId(authenticationData.getCredentialId())
				.filter(cred -> cred.getAdminAccount().getId().equals(account.getId()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown credential"));

		AAGUID aaguid = new AAGUID(storedCredential.getAaguid());
		COSEKey coseKey = objectConverter.getCborMapper().readValue(storedCredential.getPublicKeyCose(), COSEKey.class);
		AttestedCredentialData attestedCredentialData = new AttestedCredentialData(aaguid,
				storedCredential.getCredentialId(), coseKey);
		// uvInitialized/backupEligible/backupState/clientData/clientExtensions/transports aren't persisted -
		// only the counter and attested credential data are needed for signature/clone-detection verification.
		CredentialRecord credentialRecord = new CredentialRecordImpl(null, null, null, null,
				storedCredential.getSignatureCount(), attestedCredentialData, null, null, null, null);

		ServerProperty serverProperty = ServerProperty.builder().origin(origin).rpId(relyingPartyId).challenge(challenge)
				.build();
		AuthenticationParameters authenticationParameters = new AuthenticationParameters(serverProperty,
				credentialRecord, null, true);

		try {
			webAuthnManager.verify(authenticationData, authenticationParameters);
		} catch (VerificationException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WebAuthn verification failed", e);
		}

		storedCredential.setSignatureCount(authenticationData.getAuthenticatorData().getSignCount());
		storedCredential.setLastUsedAt(Instant.now());
		webAuthnCredentialRepository.save(storedCredential);
	}
}
