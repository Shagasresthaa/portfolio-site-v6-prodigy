package com.sresthaa.publicui.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sresthaa.publicui.model.Blog;
import com.sresthaa.publicui.repository.BlogRepository;
import com.sresthaa.publicui.service.UiShellClient;

// UiShellClient is mocked - the real one makes a network call to the ui service, which doesn't
// exist in this test context (same reasoning as R2StorageService being mocked elsewhere).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlogPreviewControllerTest {

	private static final String SHELL = "<!doctype html><html><head><title>Home Page</title></head>"
			+ "<body><div id=\"app\"></div><script type=\"module\" src=\"/src/main.ts\"></script></body></html>";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BlogRepository blogRepository;

	@MockitoBean
	private UiShellClient uiShellClient;

	@Test
	void blogListPassesShellThroughUnmodified() throws Exception {
		when(uiShellClient.fetchIndexHtml()).thenReturn(SHELL);

		mockMvc.perform(get("/blog"))
				.andExpect(status().isOk())
				.andExpect(content().string(SHELL));
	}

	@Test
	void blogPostInjectsRealPostMeta() throws Exception {
		when(uiShellClient.fetchIndexHtml()).thenReturn(SHELL);
		Blog blog = new Blog("preview-post", "Preview Post Title", "Preview excerpt", "content",
				"https://cdn.example/cover.webp", true, Instant.now(), "", 0, 0);
		blogRepository.save(blog);

		mockMvc.perform(get("/blog/preview-post"))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.allOf(
						Matchers.containsString("<title>Preview Post Title</title>"),
						Matchers.containsString("<meta property=\"og:title\" content=\"Preview Post Title\">"),
						Matchers.containsString("<meta property=\"og:image\" content=\"https://cdn.example/cover.webp\">"),
						Matchers.containsString("<div id=\"app\"></div>"))));
	}

	@Test
	void blogPostFallsBackToShellForUnknownSlug() throws Exception {
		when(uiShellClient.fetchIndexHtml()).thenReturn(SHELL);

		mockMvc.perform(get("/blog/no-such-post"))
				.andExpect(status().isOk())
				.andExpect(content().string(SHELL));
	}

	@Test
	void blogPostFallsBackToShellForUnpublishedSlug() throws Exception {
		when(uiShellClient.fetchIndexHtml()).thenReturn(SHELL);
		Blog blog = new Blog("draft-post", "Draft Title", "Draft excerpt", "content", null, false, null, "", 0, 0);
		blogRepository.save(blog);

		mockMvc.perform(get("/blog/draft-post"))
				.andExpect(status().isOk())
				.andExpect(content().string(SHELL));
	}
}
