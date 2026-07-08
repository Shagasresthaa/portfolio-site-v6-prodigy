package com.sresthaa.publicui.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sresthaa.publicui.dto.BlogSummary;

class BlogPreviewServiceTest {

	private final BlogPreviewService service = new BlogPreviewService();

	private static final String SHELL = "<!doctype html><html><head><title>Home Page</title></head>"
			+ "<body><div id=\"app\"></div><script type=\"module\" src=\"/src/main.ts\"></script></body></html>";

	@Test
	void injectsTitleAndOgTagsForPostWithImage() {
		BlogSummary post = new BlogSummary(UUID.randomUUID(), "my-post", "My Post Title", "A short excerpt", "content",
				"https://cdn.example/cover.webp", true, Instant.now(), "", 0, 0);

		String result = service.withPostMeta(SHELL, post, "https://www.sresthaa.com/blog/my-post");

		assertTrue(result.contains("<title>My Post Title</title>"));
		assertTrue(result.contains("<meta property=\"og:title\" content=\"My Post Title\">"));
		assertTrue(result.contains("<meta property=\"og:description\" content=\"A short excerpt\">"));
		assertTrue(result.contains("<meta property=\"og:url\" content=\"https://www.sresthaa.com/blog/my-post\">"));
		assertTrue(result.contains("<meta property=\"og:image\" content=\"https://cdn.example/cover.webp\">"));
		assertTrue(result.contains("<meta name=\"twitter:card\" content=\"summary_large_image\">"));
		assertTrue(result.contains("<div id=\"app\"></div>"));
	}

	@Test
	void omitsOgImageAndUsesSummaryCardWhenNoCoverImage() {
		BlogSummary post = new BlogSummary(UUID.randomUUID(), "no-image", "No Image Post", "Excerpt", "content", null,
				true, Instant.now(), "", 0, 0);

		String result = service.withPostMeta(SHELL, post, "https://www.sresthaa.com/blog/no-image");

		assertFalse(result.contains("og:image"));
		assertTrue(result.contains("<meta name=\"twitter:card\" content=\"summary\">"));
	}

	@Test
	void escapesHtmlSpecialCharactersInTitleAndDescription() {
		BlogSummary post = new BlogSummary(UUID.randomUUID(), "escaping", "Title with <script> & \"quotes\"",
				"Excerpt with <b>tags</b>", "content", null, true, Instant.now(), "", 0, 0);

		String result = service.withPostMeta(SHELL, post, "https://www.sresthaa.com/blog/escaping");

		assertTrue(result.contains("<title>Title with &lt;script&gt; &amp; &quot;quotes&quot;</title>"));
		assertTrue(result.contains("content=\"Excerpt with &lt;b&gt;tags&lt;/b&gt;\""));
	}
}
