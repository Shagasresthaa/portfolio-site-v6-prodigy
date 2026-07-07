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
import com.sresthaa.publicui.model.BlogImage;
import com.sresthaa.publicui.repository.BlogImageRepository;
import com.sresthaa.storage.R2StorageService;

// R2StorageService is mocked - see ImageUploadControllerTest for why (real one talks to an
// actual Cloudflare bucket over the network).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlogImageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BlogImageRepository blogImageRepository;

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
	void listAllRequiresAuth() throws Exception {
		mockMvc.perform(get("/api/admin/blog/images")).andExpect(status().is4xxClientError());
	}

	@Test
	void listAllReturnsNewestFirst() throws Exception {
		blogImageRepository.save(new BlogImage("https://cdn.example/first.webp", null));
		blogImageRepository.save(new BlogImage("https://cdn.example/second.webp", "Second"));

		mockMvc.perform(get("/api/admin/blog/images").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].url").value("https://cdn.example/second.webp"))
				.andExpect(jsonPath("$[0].altText").value("Second"))
				.andExpect(jsonPath("$[1].altText").doesNotExist());
	}

	@Test
	void createRejectsMissingToken() throws Exception {
		mockMvc.perform(post("/api/admin/blog/images")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://cdn.example/a.webp\"}"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void createRejectsBlankUrl() throws Exception {
		mockMvc.perform(post("/api/admin/blog/images")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"  \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createSucceedsAndPersists() throws Exception {
		mockMvc.perform(post("/api/admin/blog/images")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://cdn.example/a.webp\",\"altText\":\"A picture\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("https://cdn.example/a.webp"))
				.andExpect(jsonPath("$.altText").value("A picture"));

		assertEquals(1, blogImageRepository.findAll().size());
	}

	@Test
	void deleteRemovesRecordAndR2Object() throws Exception {
		BlogImage saved = blogImageRepository.save(new BlogImage("https://cdn.example/a.webp", null));

		mockMvc.perform(delete("/api/admin/blog/images/" + saved.getId()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		assertTrue(blogImageRepository.findById(saved.getId()).isEmpty());
		verify(r2StorageService).delete("https://cdn.example/a.webp");
	}

	@Test
	void deleteRejectsUnknownId() throws Exception {
		mockMvc.perform(delete("/api/admin/blog/images/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}
}
