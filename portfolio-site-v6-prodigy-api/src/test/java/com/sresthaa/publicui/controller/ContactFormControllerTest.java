package com.sresthaa.publicui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import com.sresthaa.publicui.model.ContactForm;
import com.sresthaa.publicui.repository.ContactFormRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContactFormControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContactFormRepository contactFormRepository;

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
	void submitRejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/contact")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"not-an-email\",\"message\":\"hello\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void submitRejectsBlankMessage() throws Exception {
		mockMvc.perform(post("/api/contact")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"visitor@example.com\",\"message\":\"   \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void submitAllowsAnonymousSubmissionAndPersists() throws Exception {
		mockMvc.perform(post("/api/contact")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"visitor@example.com\",\"subject\":\"Hi\",\"message\":\"hello there\"}"))
				.andExpect(status().isOk());

		ContactForm saved = contactFormRepository.findAllByOrderByCreatedAtDesc().get(0);
		assertEquals("visitor@example.com", saved.getEmail());
		assertNull(saved.getName());
		assertFalse(saved.isRead());
	}

	@Test
	void adminEndpointsRejectMissingToken() throws Exception {
		mockMvc.perform(get("/api/admin/contact")).andExpect(status().is4xxClientError());
	}

	@Test
	void listMessagesReturnsAllOrderedNewestFirst() throws Exception {
		contactFormRepository.save(new ContactForm("Alice", "alice@example.com", null, "first message"));
		contactFormRepository.save(new ContactForm("Bob", "bob@example.com", null, "second message"));

		mockMvc.perform(get("/api/admin/contact").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].email").value("bob@example.com"))
				.andExpect(jsonPath("$[1].email").value("alice@example.com"));
	}

	@Test
	void markAsReadSetsReadTrue() throws Exception {
		ContactForm saved = contactFormRepository
				.save(new ContactForm("Alice", "alice@example.com", null, "first message"));

		mockMvc.perform(post("/api/admin/contact/" + saved.getId() + "/read")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		assertTrue(contactFormRepository.findById(saved.getId()).orElseThrow().isRead());
	}

	@Test
	void markAsReadRejectsUnknownId() throws Exception {
		mockMvc.perform(post("/api/admin/contact/" + UUID.randomUUID() + "/read")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteMessageRemovesRecord() throws Exception {
		ContactForm saved = contactFormRepository
				.save(new ContactForm("Alice", "alice@example.com", null, "first message"));

		mockMvc.perform(delete("/api/admin/contact/" + saved.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		assertTrue(contactFormRepository.findById(saved.getId()).isEmpty());
	}

	@Test
	void deleteMessageRejectsUnknownId() throws Exception {
		mockMvc.perform(delete("/api/admin/contact/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}
}
