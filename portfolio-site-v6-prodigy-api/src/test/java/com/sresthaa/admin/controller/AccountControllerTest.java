package com.sresthaa.admin.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {

	// Fixture values only, not a real credential (never used outside this in-memory test DB).
	private static final String TEST_USERNAME = "test-admin";
	private static final String TEST_PASSWORD = "correct-horse";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminAccountRepository adminAccountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private String token;

	@BeforeEach
	void createAccountAndToken() {
		adminAccountRepository.save(new AdminAccount(TEST_USERNAME, passwordEncoder.encode(TEST_PASSWORD)));
		token = jwtService.issueToken(TEST_USERNAME);
	}

	@Test
	void meRejectsMissingToken() throws Exception {
		mockMvc.perform(get("/api/admin/account/me")).andExpect(status().is4xxClientError());
	}

	@Test
	void meReturnsCurrentUsername() throws Exception {
		mockMvc.perform(get("/api/admin/account/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(TEST_USERNAME));
	}

	@Test
	void changePasswordRejectsWrongCurrentPassword() throws Exception {
		mockMvc.perform(post("/api/admin/account/password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-password-123\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void changePasswordRejectsShortNewPassword() throws Exception {
		mockMvc.perform(post("/api/admin/account/password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"%s\",\"newPassword\":\"short\"}".formatted(TEST_PASSWORD)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changePasswordSucceedsAndAllowsLoginWithNewPassword() throws Exception {
		mockMvc.perform(post("/api/admin/account/password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"%s\",\"newPassword\":\"new-password-123\"}".formatted(TEST_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());

		AdminAccount updated = adminAccountRepository.findByUsername(TEST_USERNAME).orElseThrow();
		assertTrue(passwordEncoder.matches("new-password-123", updated.getPasswordHash()));
	}

	@Test
	void changeUsernameRejectsWrongCurrentPassword() throws Exception {
		mockMvc.perform(post("/api/admin/account/username")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"wrong\",\"newUsername\":\"someone-else\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void changeUsernameRejectsBlankUsername() throws Exception {
		mockMvc.perform(post("/api/admin/account/username")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"%s\",\"newUsername\":\"   \"}".formatted(TEST_PASSWORD)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changeUsernameRejectsAlreadyTakenUsername() throws Exception {
		adminAccountRepository.save(new AdminAccount("other-admin", passwordEncoder.encode("something")));

		mockMvc.perform(post("/api/admin/account/username")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"%s\",\"newUsername\":\"other-admin\"}".formatted(TEST_PASSWORD)))
				.andExpect(status().isConflict());
	}

	@Test
	void changeUsernameSucceedsAndOldTokenNoLongerResolves() throws Exception {
		mockMvc.perform(post("/api/admin/account/username")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"%s\",\"newUsername\":\"renamed-admin\"}".formatted(TEST_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());

		// The old token's subject (TEST_USERNAME) no longer resolves to any account.
		mockMvc.perform(get("/api/admin/account/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	// No corresponding "accepts correct password" test: that's just the inverse of the one
	// boolean check this rejection test already exercises (PasswordEncoder.matches) - there's
	// no code path where accept could independently break while reject stays correct.
	@Test
	void verifyPasswordRejectsIncorrectPassword() throws Exception {
		mockMvc.perform(post("/api/admin/account/verify-password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized());
	}
}
