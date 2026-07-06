package com.sresthaa.publicui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.sresthaa.publicui.model.Highlight;
import com.sresthaa.publicui.model.HighlightMediaType;
import com.sresthaa.publicui.repository.HighlightRepository;
import com.sresthaa.storage.R2StorageService;

// R2StorageService is mocked - see ImageUploadControllerTest for why (real one talks to an
// actual Cloudflare bucket over the network).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HighlightControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private HighlightRepository highlightRepository;

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
	void listHighlightsIsPublicAndReturnsNewestFirst() throws Exception {
		highlightRepository.save(new Highlight("First", null, null, HighlightMediaType.VIDEO, null, null,
				"https://www.youtube.com/watch?v=abc", "tag1"));
		highlightRepository.save(new Highlight("Second", null, null, HighlightMediaType.VIDEO, null, null,
				"https://www.youtube.com/watch?v=def", "tag1"));

		mockMvc.perform(get("/api/highlights"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].title").value("Second"))
				.andExpect(jsonPath("$[1].title").value("First"));
	}

	@Test
	void createRejectsMissingToken() throws Exception {
		mockMvc.perform(post("/api/admin/highlights")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void createRejectsBlankTitle() throws Exception {
		mockMvc.perform(post("/api/admin/highlights")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"  \",\"tags\":\"tag1\",\"mediaType\":\"VIDEO\","
						+ "\"videoUrl\":\"https://www.youtube.com/watch?v=abc\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createImageHighlightRejectsMissingImage() throws Exception {
		mockMvc.perform(post("/api/admin/highlights")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"A moment\",\"tags\":\"tag1\",\"mediaType\":\"IMAGE\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createVideoHighlightRejectsMissingVideoUrl() throws Exception {
		mockMvc.perform(post("/api/admin/highlights")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"A moment\",\"tags\":\"tag1\",\"mediaType\":\"VIDEO\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createImageHighlightSucceedsAndPersists() throws Exception {
		mockMvc.perform(post("/api/admin/highlights")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"A moment\",\"tags\":\"tag1, tag2\",\"mediaType\":\"IMAGE\","
						+ "\"image\":\"https://cdn.example/full.webp\",\"thumbnailImage\":\"https://cdn.example/thumb.webp\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("A moment"))
				.andExpect(jsonPath("$.image").value("https://cdn.example/full.webp"));

		assertEquals(1, highlightRepository.findAllByOrderByCreatedAtDesc().size());
	}

	@Test
	void createVideoHighlightSucceedsAndPersists() throws Exception {
		mockMvc.perform(post("/api/admin/highlights")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"A talk\",\"tags\":\"tag1\",\"mediaType\":\"VIDEO\","
						+ "\"videoUrl\":\"https://www.youtube.com/watch?v=abc\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.videoUrl").value("https://www.youtube.com/watch?v=abc"));

		assertEquals(1, highlightRepository.findAllByOrderByCreatedAtDesc().size());
	}

	@Test
	void updateModifiesExistingHighlight() throws Exception {
		Highlight saved = highlightRepository.save(new Highlight("Original", null, null, HighlightMediaType.VIDEO,
				null, null, "https://www.youtube.com/watch?v=abc", "tag1"));

		mockMvc.perform(put("/api/admin/highlights/" + saved.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Updated\",\"tags\":\"tag1\",\"mediaType\":\"VIDEO\","
						+ "\"videoUrl\":\"https://www.youtube.com/watch?v=abc\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Updated"));

		assertEquals("Updated", highlightRepository.findById(saved.getId()).orElseThrow().getTitle());
	}

	@Test
	void updateRejectsUnknownId() throws Exception {
		mockMvc.perform(put("/api/admin/highlights/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Updated\",\"tags\":\"tag1\",\"mediaType\":\"VIDEO\","
						+ "\"videoUrl\":\"https://www.youtube.com/watch?v=abc\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteRemovesHighlight() throws Exception {
		Highlight saved = highlightRepository.save(new Highlight("Original", null, null, HighlightMediaType.VIDEO,
				null, null, "https://www.youtube.com/watch?v=abc", "tag1"));

		mockMvc.perform(delete("/api/admin/highlights/" + saved.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		assertTrue(highlightRepository.findById(saved.getId()).isEmpty());
	}

	@Test
	void deleteRejectsUnknownId() throws Exception {
		mockMvc.perform(delete("/api/admin/highlights/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteImageHighlightRemovesBothR2Objects() throws Exception {
		Highlight saved = highlightRepository.save(new Highlight("A moment", null, null, HighlightMediaType.IMAGE,
				"https://cdn.example/full.webp", "https://cdn.example/thumb.webp", null, "tag1"));

		mockMvc.perform(delete("/api/admin/highlights/" + saved.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		verify(r2StorageService).delete("https://cdn.example/full.webp");
		verify(r2StorageService).delete("https://cdn.example/thumb.webp");
	}

	@Test
	void deleteVideoHighlightNeverCallsR2() throws Exception {
		Highlight saved = highlightRepository.save(new Highlight("Original", null, null, HighlightMediaType.VIDEO,
				null, null, "https://www.youtube.com/watch?v=abc", "tag1"));

		mockMvc.perform(delete("/api/admin/highlights/" + saved.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		verify(r2StorageService, never()).delete(anyString());
	}

	@Test
	void updateReplacingImageDeletesOnlyTheOldR2Objects() throws Exception {
		Highlight saved = highlightRepository.save(new Highlight("A moment", null, null, HighlightMediaType.IMAGE,
				"https://cdn.example/old-full.webp", "https://cdn.example/old-thumb.webp", null, "tag1"));

		mockMvc.perform(put("/api/admin/highlights/" + saved.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"A moment\",\"tags\":\"tag1\",\"mediaType\":\"IMAGE\","
						+ "\"image\":\"https://cdn.example/new-full.webp\","
						+ "\"thumbnailImage\":\"https://cdn.example/new-thumb.webp\"}"))
				.andExpect(status().isOk());

		verify(r2StorageService).delete("https://cdn.example/old-full.webp");
		verify(r2StorageService).delete("https://cdn.example/old-thumb.webp");
		verify(r2StorageService, never()).delete("https://cdn.example/new-full.webp");
		verify(r2StorageService, never()).delete("https://cdn.example/new-thumb.webp");
	}

	@Test
	void updateKeepingSameImageDoesNotDeleteAnyR2Object() throws Exception {
		Highlight saved = highlightRepository.save(new Highlight("A moment", null, null, HighlightMediaType.IMAGE,
				"https://cdn.example/full.webp", "https://cdn.example/thumb.webp", null, "tag1"));

		mockMvc.perform(put("/api/admin/highlights/" + saved.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Renamed\",\"tags\":\"tag1\",\"mediaType\":\"IMAGE\","
						+ "\"image\":\"https://cdn.example/full.webp\","
						+ "\"thumbnailImage\":\"https://cdn.example/thumb.webp\"}"))
				.andExpect(status().isOk());

		verify(r2StorageService, never()).delete(anyString());
	}
}
