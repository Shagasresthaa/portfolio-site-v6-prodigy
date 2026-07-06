package com.sresthaa.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.model.SystemOverride;
import com.sresthaa.admin.model.SystemOverrideKeys;
import com.sresthaa.admin.model.WebAuthnCredential;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.repository.SystemOverrideRepository;
import com.sresthaa.admin.repository.WebAuthnCredentialRepository;
import com.sresthaa.admin.security.JwtService;
import com.sresthaa.admin.webauthn.WebAuthnChallengeStore;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebAuthnControllerTest {

	// Fixture values only, not a real credential (never used outside this in-memory test DB).
	private static final String TEST_USERNAME = "test-admin";
	private static final String TEST_PASSWORD = "correct-horse";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminAccountRepository adminAccountRepository;

	@Autowired
	private SystemOverrideRepository systemOverrideRepository;

	@Autowired
	private WebAuthnCredentialRepository webAuthnCredentialRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	// In-memory singleton bean - @Transactional only rolls back the DB, so stale challenges from
	// a previous test method (same username, every test here) would otherwise leak across tests.
	@Autowired
	private WebAuthnChallengeStore challengeStore;

	private AdminAccount account;
	private String token;

	@BeforeEach
	void createAccountAndToken() {
		account = adminAccountRepository
				.save(new AdminAccount(TEST_USERNAME, passwordEncoder.encode(TEST_PASSWORD)));
		systemOverrideRepository.save(new SystemOverride(SystemOverrideKeys.BYPASS_SECOND_FACTOR, "true"));
		token = jwtService.issueToken(account.getUsername());
		challengeStore.consumeRegistrationChallenge(TEST_USERNAME);
		challengeStore.consumeAuthenticationChallenge(TEST_USERNAME);
	}

	@Test
	void registrationOptionsRejectsMissingToken() throws Exception {
		mockMvc.perform(post("/api/admin/webauthn/register/options"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void registrationOptionsReturnsChallengeAndRelyingParty() throws Exception {
		mockMvc.perform(post("/api/admin/webauthn/register/options")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.challenge").isNotEmpty())
				.andExpect(jsonPath("$.rp").exists())
				.andExpect(jsonPath("$.user").exists());
	}

	@Test
	void registrationVerifyRejectsWithoutPendingChallenge() throws Exception {
		mockMvc.perform(post("/api/admin/webauthn/register/verify")
				.header("Authorization", "Bearer " + token)
				.param("label", "My Key")
				.contentType(MediaType.TEXT_PLAIN)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listCredentialsRejectsMissingToken() throws Exception {
		mockMvc.perform(get("/api/admin/webauthn/credentials"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void listCredentialsReturnsOnlyOwnAccountsKeys() throws Exception {
		AdminAccount otherAccount = adminAccountRepository
				.save(new AdminAccount("other-admin", passwordEncoder.encode("something")));
		webAuthnCredentialRepository.save(new WebAuthnCredential(account, new byte[16], new byte[] { 1, 2, 3 },
				new byte[] { 4, 5, 6 }, 0L, null, "My Key"));
		webAuthnCredentialRepository.save(new WebAuthnCredential(otherAccount, new byte[16], new byte[] { 7, 8, 9 },
				new byte[] { 10, 11, 12 }, 0L, null, "Other's Key"));

		mockMvc.perform(get("/api/admin/webauthn/credentials")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].label").value("My Key"));
	}

	@Test
	void deleteCredentialRemovesOwnKey() throws Exception {
		WebAuthnCredential credential = webAuthnCredentialRepository.save(new WebAuthnCredential(account,
				new byte[16], new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 }, 0L, null, "My Key"));

		mockMvc.perform(delete("/api/admin/webauthn/credentials/" + credential.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/admin/webauthn/credentials")
				.header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void deleteCredentialRejectsAnotherAccountsKey() throws Exception {
		AdminAccount otherAccount = adminAccountRepository
				.save(new AdminAccount("other-admin", passwordEncoder.encode("something")));
		WebAuthnCredential credential = webAuthnCredentialRepository.save(new WebAuthnCredential(otherAccount,
				new byte[16], new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 }, 0L, null, "Other's Key"));

		mockMvc.perform(delete("/api/admin/webauthn/credentials/" + credential.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteCredentialRejectsUnknownId() throws Exception {
		mockMvc.perform(delete("/api/admin/webauthn/credentials/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}
}
