package com.sresthaa.publicui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.sresthaa.publicui.repository.HomeContentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HomeContentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private HomeContentRepository homeContentRepository;

	@Autowired
	private AdminAccountRepository adminAccountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private String token;

	@BeforeEach
	void createAdminToken() {
		adminAccountRepository.save(new AdminAccount("test-admin", passwordEncoder.encode("correct-horse")));
		token = jwtService.issueToken("test-admin");
	}

	@Test
	void getReturnsEmptyDefaultsWhenNoContentSaved() throws Exception {
		// Defensive clear (test DB should already be empty) - rolled back after this test.
		homeContentRepository.deleteAll();

		mockMvc.perform(get("/api/home"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aboutHook").value(""))
				.andExpect(jsonPath("$.aboutStory.length()").value(0))
				.andExpect(jsonPath("$.timeline.length()").value(0));
	}

	@Test
	void updateRejectsMissingToken() throws Exception {
		mockMvc.perform(put("/api/admin/home")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"aboutHook\":\"Hi\",\"aboutStory\":[],\"timeline\":[]}"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void updateRejectsMissingFields() throws Exception {
		mockMvc.perform(put("/api/admin/home")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"aboutHook\":\"Hi\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateCreatesContentAndGetReflectsIt() throws Exception {
		mockMvc.perform(put("/api/admin/home")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"aboutHook\":\"Hi there\",\"aboutStory\":[\"Paragraph one\",\"Paragraph two\"],"
						+ "\"timeline\":[{\"title\":\"Graduated\",\"position\":\"Student\",\"institution\":\"WMU\","
						+ "\"date\":\"2026\",\"duration\":\"\"}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aboutHook").value("Hi there"));

		mockMvc.perform(get("/api/home"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aboutHook").value("Hi there"))
				.andExpect(jsonPath("$.aboutStory[0]").value("Paragraph one"))
				.andExpect(jsonPath("$.aboutStory[1]").value("Paragraph two"))
				.andExpect(jsonPath("$.timeline[0].title").value("Graduated"))
				.andExpect(jsonPath("$.timeline[0].institution").value("WMU"));

		assertEquals(1, homeContentRepository.findAll().size());
	}

	@Test
	void updateAndGetRoundTripResumeUrl() throws Exception {
		mockMvc.perform(put("/api/admin/home")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"aboutHook\":\"Hi\",\"aboutStory\":[],\"timeline\":[],"
						+ "\"resumeUrl\":\"https://cdn.example/resume/latest.pdf\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumeUrl").value("https://cdn.example/resume/latest.pdf"));

		mockMvc.perform(get("/api/home"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumeUrl").value("https://cdn.example/resume/latest.pdf"));
	}

	@Test
	void updateReplacesRatherThanDuplicatingTheSingletonRow() throws Exception {
		mockMvc.perform(put("/api/admin/home")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"aboutHook\":\"First\",\"aboutStory\":[],\"timeline\":[]}"))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/admin/home")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"aboutHook\":\"Second\",\"aboutStory\":[],\"timeline\":[]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aboutHook").value("Second"));

		assertEquals(1, homeContentRepository.findAll().size());
	}
}
