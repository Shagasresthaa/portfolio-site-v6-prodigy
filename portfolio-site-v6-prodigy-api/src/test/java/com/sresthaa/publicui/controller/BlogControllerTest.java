package com.sresthaa.publicui.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.sresthaa.publicui.model.Blog;
import com.sresthaa.publicui.model.BlogComment;
import com.sresthaa.publicui.repository.BlogCommentRepository;
import com.sresthaa.publicui.repository.BlogRepository;
import com.sresthaa.storage.R2StorageService;

// R2StorageService is mocked - see ImageUploadControllerTest for why (real one talks to an
// actual Cloudflare bucket over the network).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BlogRepository blogRepository;

	@Autowired
	private BlogCommentRepository blogCommentRepository;

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

	private Blog publishedPost(String slug, String title, Instant publishedAt) {
		return new Blog(slug, title, "excerpt", "content", null, true, publishedAt, "tag1", 0, 0);
	}

	private Blog draftPost(String slug, String title) {
		return new Blog(slug, title, "excerpt", "content", null, false, null, "tag1", 0, 0);
	}

	@Test
	void listPublishedOnlyReturnsPublishedNewestFirst() throws Exception {
		blogRepository.save(publishedPost("first", "First", Instant.parse("2025-01-01T00:00:00Z")));
		blogRepository.save(publishedPost("second", "Second", Instant.parse("2026-01-01T00:00:00Z")));
		blogRepository.save(draftPost("draft", "Draft"));

		mockMvc.perform(get("/api/blog"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].slug").value("second"))
				.andExpect(jsonPath("$[1].slug").value("first"));
	}

	@Test
	void getBySlugReturnsPublishedPost() throws Exception {
		blogRepository.save(publishedPost("hello-world", "Hello World", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(get("/api/blog/hello-world"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Hello World"));
	}

	@Test
	void getBySlugRejectsUnpublishedPost() throws Exception {
		blogRepository.save(draftPost("draft", "Draft"));

		mockMvc.perform(get("/api/blog/draft")).andExpect(status().isNotFound());
	}

	@Test
	void getBySlugRejectsUnknownSlug() throws Exception {
		mockMvc.perform(get("/api/blog/does-not-exist")).andExpect(status().isNotFound());
	}

	@Test
	void listAllIncludesDraftsAndRequiresAuth() throws Exception {
		blogRepository.save(publishedPost("first", "First", Instant.parse("2026-01-01T00:00:00Z")));
		blogRepository.save(draftPost("draft", "Draft"));

		mockMvc.perform(get("/api/admin/blog")).andExpect(status().is4xxClientError());

		mockMvc.perform(get("/api/admin/blog").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void createRejectsMissingToken() throws Exception {
		mockMvc.perform(post("/api/admin/blog").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void createRejectsBlankRequiredFields() throws Exception {
		mockMvc.perform(post("/api/admin/blog")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"\",\"title\":\"\",\"excerpt\":\"\",\"content\":\"\",\"published\":false}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createRejectsDuplicateSlug() throws Exception {
		blogRepository.save(draftPost("taken", "Original"));

		mockMvc.perform(post("/api/admin/blog")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("taken", "New Post", false)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createUnpublishedLeavesPublishedAtNull() throws Exception {
		mockMvc.perform(post("/api/admin/blog")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("draft-post", "Draft Post", false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.published").value(false))
				.andExpect(jsonPath("$.publishedAt").doesNotExist());
	}

	@Test
	void createPublishedSetsPublishedAt() throws Exception {
		mockMvc.perform(post("/api/admin/blog")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("live-post", "Live Post", true)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.published").value(true))
				.andExpect(jsonPath("$.publishedAt").exists());

		assertEquals(1, blogRepository.findAll().size());
	}

	@Test
	void updatingAlreadyPublishedPostDoesNotResetPublishedAt() throws Exception {
		Blog saved = blogRepository.save(publishedPost("live-post", "Live Post", Instant.parse("2025-06-01T00:00:00Z")));

		mockMvc.perform(put("/api/admin/blog/" + saved.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("live-post", "Live Post Updated", true)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishedAt").value("2025-06-01T00:00:00Z"));
	}

	@Test
	void updateRejectsUnknownId() throws Exception {
		mockMvc.perform(put("/api/admin/blog/" + UUID.randomUUID())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("some-slug", "Some Title", false)))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateRejectsSlugAlreadyUsedByAnotherPost() throws Exception {
		blogRepository.save(draftPost("taken", "Taken"));
		Blog saved = blogRepository.save(draftPost("mine", "Mine"));

		mockMvc.perform(put("/api/admin/blog/" + saved.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("taken", "Mine", false)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateKeepingItsOwnSlugSucceeds() throws Exception {
		Blog saved = blogRepository.save(draftPost("mine", "Mine"));

		mockMvc.perform(put("/api/admin/blog/" + saved.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson("mine", "Mine Updated", false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Mine Updated"));
	}

	@Test
	void deleteRemovesPostAndCascadesComments() throws Exception {
		Blog saved = blogRepository.save(publishedPost("live-post", "Live Post", Instant.parse("2026-01-01T00:00:00Z")));
		blogCommentRepository.save(new BlogComment(saved, "Commenter", "Nice post"));

		mockMvc.perform(delete("/api/admin/blog/" + saved.getId()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		assertTrue(blogRepository.findById(saved.getId()).isEmpty());
		assertTrue(blogCommentRepository.findAllByBlogIdOrderByCreatedAtDesc(saved.getId()).isEmpty());
	}

	@Test
	void deleteRejectsUnknownId() throws Exception {
		mockMvc.perform(delete("/api/admin/blog/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteRemovesCoverImageFromR2() throws Exception {
		Blog saved = blogRepository.save(new Blog("live-post", "Live Post", "excerpt", "content",
				"https://cdn.example/cover.webp", true, Instant.parse("2026-01-01T00:00:00Z"), "tag1", 0, 0));

		mockMvc.perform(delete("/api/admin/blog/" + saved.getId()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		org.mockito.Mockito.verify(r2StorageService).delete("https://cdn.example/cover.webp");
	}

	@Test
	void listCommentsRejectsUnpublishedSlug() throws Exception {
		blogRepository.save(draftPost("draft", "Draft"));

		mockMvc.perform(get("/api/blog/draft/comments")).andExpect(status().isNotFound());
	}

	@Test
	void addCommentSucceedsAndListReturnsNewestFirst() throws Exception {
		blogRepository.save(publishedPost("live-post", "Live Post", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(post("/api/blog/live-post/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Alice\",\"content\":\"First!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Alice"));

		mockMvc.perform(post("/api/blog/live-post/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"Anonymous comment\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").doesNotExist());

		mockMvc.perform(get("/api/blog/live-post/comments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].content").value("Anonymous comment"))
				.andExpect(jsonPath("$[1].content").value("First!"));
	}

	@Test
	void addCommentRejectsBlankContent() throws Exception {
		blogRepository.save(publishedPost("live-post", "Live Post", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(post("/api/blog/live-post/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"  \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void addCommentRejectsOverlongName() throws Exception {
		blogRepository.save(publishedPost("live-post", "Live Post", Instant.parse("2026-01-01T00:00:00Z")));

		String longName = "a".repeat(101);
		mockMvc.perform(post("/api/blog/live-post/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + longName + "\",\"content\":\"hi\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void addCommentRejectsUnpublishedSlug() throws Exception {
		blogRepository.save(draftPost("draft", "Draft"));

		mockMvc.perform(post("/api/blog/draft/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"hi\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void applyReactionAddsLike() throws Exception {
		blogRepository.save(publishedPost("live-post", "Live Post", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(post("/api/blog/live-post/reactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"previousVote\":null,\"vote\":\"LIKE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(1))
				.andExpect(jsonPath("$.dislikeCount").value(0));
	}

	@Test
	void applyReactionSwitchesFromLikeToDislike() throws Exception {
		blogRepository.save(publishedPost("live-post", "Live Post", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(post("/api/blog/live-post/reactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"previousVote\":\"LIKE\",\"vote\":\"DISLIKE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(0))
				.andExpect(jsonPath("$.dislikeCount").value(1));
	}

	@Test
	void applyReactionRemovingVoteNeverGoesNegative() throws Exception {
		blogRepository.save(publishedPost("live-post", "Live Post", Instant.parse("2026-01-01T00:00:00Z")));

		mockMvc.perform(post("/api/blog/live-post/reactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"previousVote\":\"LIKE\",\"vote\":null}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likeCount").value(0))
				.andExpect(jsonPath("$.dislikeCount").value(0));
	}

	@Test
	void applyReactionRejectsUnpublishedSlug() throws Exception {
		blogRepository.save(draftPost("draft", "Draft"));

		mockMvc.perform(post("/api/blog/draft/reactions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"previousVote\":null,\"vote\":\"LIKE\"}"))
				.andExpect(status().isNotFound());
	}

	private String postJson(String slug, String title, boolean published) {
		return "{\"slug\":\"" + slug + "\",\"title\":\"" + title
				+ "\",\"excerpt\":\"excerpt\",\"content\":\"content\",\"tags\":\"tag1\",\"published\":" + published + "}";
	}
}
