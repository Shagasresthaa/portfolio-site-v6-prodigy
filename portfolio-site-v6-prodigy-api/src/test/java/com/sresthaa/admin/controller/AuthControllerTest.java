package com.sresthaa.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.sresthaa.admin.webauthn.WebAuthnChallengeStore;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

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

	// In-memory singleton bean - @Transactional only rolls back the DB, so stale challenges from
	// a previous test method (same username, every test here) would otherwise leak across tests.
	@Autowired
	private WebAuthnChallengeStore challengeStore;

	private AdminAccount account;

	@BeforeEach
	void createAccount() {
		account = adminAccountRepository.save(new AdminAccount("test-admin", passwordEncoder.encode("correct-horse")));
		challengeStore.consumeRegistrationChallenge("test-admin");
		challengeStore.consumeAuthenticationChallenge("test-admin");
	}

	@Test
	void rejectsWrongPassword() throws Exception {
		mockMvc.perform(post("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"test-admin\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsCorrectPasswordWithoutBypassOverride() throws Exception {
		systemOverrideRepository.deleteById(SystemOverrideKeys.BYPASS_SECOND_FACTOR);

		mockMvc.perform(post("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"test-admin\",\"password\":\"correct-horse\"}"))
				.andExpect(status().isNotImplemented());
	}

	@Test
	void issuesTokenWhenBypassOverrideEnabled() throws Exception {
		systemOverrideRepository.save(new SystemOverride(SystemOverrideKeys.BYPASS_SECOND_FACTOR, "true"));

		mockMvc.perform(post("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"test-admin\",\"password\":\"correct-horse\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void protectedAdminPathRejectsMissingToken() throws Exception {
		mockMvc.perform(get("/api/admin/does-not-exist-yet"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void respondsWebAuthnRequiredWhenAccountHasCredentialAndNoBypass() throws Exception {
		systemOverrideRepository.deleteById(SystemOverrideKeys.BYPASS_SECOND_FACTOR);
		webAuthnCredentialRepository.save(new WebAuthnCredential(account, new byte[16], new byte[] { 1, 2, 3 },
				new byte[] { 4, 5, 6 }, 0L, null, "Test Key"));

		mockMvc.perform(post("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"test-admin\",\"password\":\"correct-horse\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WEBAUTHN_REQUIRED"))
				.andExpect(jsonPath("$.webAuthnOptions").isNotEmpty())
				.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	void webAuthnVerifyRejectsWithoutPendingChallenge() throws Exception {
		mockMvc.perform(post("/api/admin/auth/webauthn/verify")
				.param("username", "test-admin")
				.contentType(MediaType.TEXT_PLAIN)
				.content("{}"))
				.andExpect(status().isUnauthorized());
	}
}
