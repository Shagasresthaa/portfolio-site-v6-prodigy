package com.sresthaa.admin.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.security.JwtService;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TotpControllerTest {

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
		AdminAccount account = adminAccountRepository
				.save(new AdminAccount("test-admin", passwordEncoder.encode("correct-horse")));
		token = jwtService.issueToken(account.getUsername());
	}

	@Test
	void statusRejectsMissingToken() throws Exception {
		mockMvc.perform(get("/api/admin/totp/status"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void statusReportsDisabledBeforeEnrollment() throws Exception {
		mockMvc.perform(get("/api/admin/totp/status").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false))
				.andExpect(jsonPath("$.remainingBackupCodes").value(0));
	}

	@Test
	void beginEnrollmentReturnsSecretAndQrCode() throws Exception {
		mockMvc.perform(post("/api/admin/totp/enroll/begin").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.secret").isNotEmpty())
				.andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")))
				.andExpect(jsonPath("$.qrCodeImagePng").isNotEmpty());
	}

	@Test
	void confirmEnrollmentRejectsInvalidCode() throws Exception {
		mockMvc.perform(post("/api/admin/totp/enroll/begin").header("Authorization", "Bearer " + token));

		mockMvc.perform(post("/api/admin/totp/enroll/confirm")
				.header("Authorization", "Bearer " + token)
				.param("code", "000000"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void confirmEnrollmentWithValidCodeEnablesAndReturnsBackupCodes() throws Exception {
		MvcResult beginResult = mockMvc
				.perform(post("/api/admin/totp/enroll/begin").header("Authorization", "Bearer " + token))
				.andReturn();
		String secret = extractSecret(beginResult.getResponse().getContentAsString());

		mockMvc.perform(post("/api/admin/totp/enroll/confirm")
				.header("Authorization", "Bearer " + token)
				.param("code", currentTotpCode(secret)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(8));

		mockMvc.perform(get("/api/admin/totp/status").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.enabled").value(true))
				.andExpect(jsonPath("$.remainingBackupCodes").value(8));
	}

	@Test
	void regenerateBackupCodesRejectsWhenNotEnabled() throws Exception {
		mockMvc.perform(post("/api/admin/totp/backup-codes/regenerate").header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
	}

	@Test
	void disableRemovesCredential() throws Exception {
		MvcResult beginResult = mockMvc
				.perform(post("/api/admin/totp/enroll/begin").header("Authorization", "Bearer " + token))
				.andReturn();
		String secret = extractSecret(beginResult.getResponse().getContentAsString());
		mockMvc.perform(post("/api/admin/totp/enroll/confirm")
				.header("Authorization", "Bearer " + token)
				.param("code", currentTotpCode(secret)));

		mockMvc.perform(delete("/api/admin/totp").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/admin/totp/status").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.enabled").value(false));
	}

	private String extractSecret(String responseBody) {
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"secret\":\"([^\"]+)\"")
				.matcher(responseBody);
		if (!matcher.find()) {
			throw new IllegalStateException("No secret found in response: " + responseBody);
		}
		return matcher.group(1);
	}

	private String currentTotpCode(String secret) throws Exception {
		SystemTimeProvider timeProvider = new SystemTimeProvider();
		long counter = Math.floorDiv(timeProvider.getTime(), 30);
		String code = new DefaultCodeGenerator().generate(secret, counter);
		assertEquals(6, code.length());
		return code;
	}
}
