package com.sresthaa.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.security.JwtService;
import com.sresthaa.storage.R2StorageService;

// R2StorageService is mocked here rather than left real - the real one talks to an actual
// Cloudflare R2 bucket over the network, which is too slow/flaky for tests and would leave
// debris in a real bucket.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImageUploadControllerTest {

	@Autowired
	private MockMvc mockMvc;

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
	void uploadRejectsMissingToken() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", new byte[] { 1, 2, 3 });

		mockMvc.perform(multipart("/api/admin/uploads/image").file(file).param("category", "highlights"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void uploadRejectsUnknownCategory() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", new byte[] { 1, 2, 3 });

		mockMvc.perform(multipart("/api/admin/uploads/image")
				.file(file)
				.param("category", "not-a-real-category")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
	}

	@Test
	void uploadRejectsNonImageFile() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[] { 1, 2, 3 });

		mockMvc.perform(multipart("/api/admin/uploads/image")
				.file(file)
				.param("category", "highlights")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
	}

	@Test
	void uploadSucceedsAndReturnsUrl() throws Exception {
		when(r2StorageService.upload(any(), anyString(), anyString()))
				.thenReturn("https://cdn.example/highlights/generated.webp");
		MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", new byte[] { 1, 2, 3 });

		mockMvc.perform(multipart("/api/admin/uploads/image")
				.file(file)
				.param("category", "highlights")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("https://cdn.example/highlights/generated.webp"));
	}
}
