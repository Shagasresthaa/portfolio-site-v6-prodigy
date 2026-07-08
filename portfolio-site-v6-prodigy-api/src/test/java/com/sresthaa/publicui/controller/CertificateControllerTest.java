package com.sresthaa.publicui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.security.JwtService;
import com.sresthaa.publicui.model.Certificate;
import com.sresthaa.publicui.repository.CertificateRepository;
import com.sresthaa.storage.R2StorageService;

// R2StorageService is mocked - see ImageUploadControllerTest for why (real one talks to an
// actual Cloudflare bucket over the network).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CertificateControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CertificateRepository certificateRepository;

	@Autowired
	private AdminAccountRepository adminAccountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private R2StorageService r2StorageService;

	private String token;

	@BeforeEach
	void createAdminToken() {
		adminAccountRepository.save(new AdminAccount("test-admin", passwordEncoder.encode("correct-horse")));
		token = jwtService.issueToken("test-admin");
	}

	@Test
	void listAllIsPublicAndReturnsNewestFirst() throws Exception {
		// This dev DB has real certificates uploaded outside of this test's transaction (via the
		// admin UI) - clear it within the transaction so the count assertion below is exact;
		// rolled back after, doesn't touch the real rows.
		certificateRepository.deleteAll();
		certificateRepository.save(new Certificate("https://cdn.example/first.webp", null));
		certificateRepository.save(new Certificate("https://cdn.example/second.webp", "AWS Certified"));

		mockMvc.perform(get("/api/certificates"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].imageUrl").value("https://cdn.example/second.webp"))
				.andExpect(jsonPath("$[0].title").value("AWS Certified"))
				.andExpect(jsonPath("$[1].title").doesNotExist());
	}

	@Test
	void createRejectsMissingToken() throws Exception {
		mockMvc.perform(post("/api/admin/certificates")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"imageUrl\":\"https://cdn.example/a.webp\"}"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void createRejectsBlankImageUrl() throws Exception {
		mockMvc.perform(post("/api/admin/certificates")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"imageUrl\":\"  \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createSucceedsAndPersists() throws Exception {
		certificateRepository.deleteAll();
		mockMvc.perform(post("/api/admin/certificates")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"imageUrl\":\"https://cdn.example/a.webp\",\"title\":\"AWS Certified\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.imageUrl").value("https://cdn.example/a.webp"))
				.andExpect(jsonPath("$.title").value("AWS Certified"));

		assertEquals(1, certificateRepository.findAll().size());
	}

	@Test
	void deleteRemovesRecordAndR2Object() throws Exception {
		Certificate saved = certificateRepository.save(new Certificate("https://cdn.example/a.webp", null));

		mockMvc.perform(delete("/api/admin/certificates/" + saved.getId()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		assertTrue(certificateRepository.findById(saved.getId()).isEmpty());
		verify(r2StorageService).delete("https://cdn.example/a.webp");
	}

	@Test
	void deleteRejectsUnknownId() throws Exception {
		mockMvc.perform(delete("/api/admin/certificates/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteRejectsMissingToken() throws Exception {
		Certificate saved = certificateRepository.save(new Certificate("https://cdn.example/a.webp", null));

		mockMvc.perform(delete("/api/admin/certificates/" + saved.getId()))
				.andExpect(status().is4xxClientError());
	}
}
