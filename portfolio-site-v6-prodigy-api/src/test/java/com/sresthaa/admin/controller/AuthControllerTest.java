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
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.repository.SystemOverrideRepository;

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
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void createAccount() {
		adminAccountRepository.save(new AdminAccount("test-admin", passwordEncoder.encode("correct-horse")));
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
}
