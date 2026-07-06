package com.sresthaa.admin.controller;

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
		adminAccountRepository.save(new AdminAccount("test-admin", passwordEncoder.encode("correct-horse")));
		token = jwtService.issueToken("test-admin");
	}

	@Test
	void meRejectsMissingToken() throws Exception {
		mockMvc.perform(get("/api/admin/account/me")).andExpect(status().is4xxClientError());
	}

	@Test
	void meReturnsCurrentUsername() throws Exception {
		mockMvc.perform(get("/api/admin/account/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("test-admin"));
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
				.content("{\"currentPassword\":\"correct-horse\",\"newPassword\":\"short\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changePasswordSucceedsAndAllowsLoginWithNewPassword() throws Exception {
		mockMvc.perform(post("/api/admin/account/password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"correct-horse\",\"newPassword\":\"new-password-123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());

		AdminAccount updated = adminAccountRepository.findByUsername("test-admin").orElseThrow();
		org.junit.jupiter.api.Assertions
				.assertTrue(passwordEncoder.matches("new-password-123", updated.getPasswordHash()));
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
				.content("{\"currentPassword\":\"correct-horse\",\"newUsername\":\"   \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changeUsernameRejectsAlreadyTakenUsername() throws Exception {
		adminAccountRepository.save(new AdminAccount("other-admin", passwordEncoder.encode("something")));

		mockMvc.perform(post("/api/admin/account/username")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"correct-horse\",\"newUsername\":\"other-admin\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void changeUsernameSucceedsAndOldTokenNoLongerResolves() throws Exception {
		mockMvc.perform(post("/api/admin/account/username")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"correct-horse\",\"newUsername\":\"renamed-admin\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());

		// The old token's subject ("test-admin") no longer resolves to any account.
		mockMvc.perform(get("/api/admin/account/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void verifyPasswordRejectsIncorrectPassword() throws Exception {
		mockMvc.perform(post("/api/admin/account/verify-password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void verifyPasswordAcceptsCorrectPassword() throws Exception {
		mockMvc.perform(post("/api/admin/account/verify-password")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"correct-horse\"}"))
				.andExpect(status().isOk());
	}
}
