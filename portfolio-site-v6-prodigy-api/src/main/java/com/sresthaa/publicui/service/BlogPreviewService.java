package com.sresthaa.publicui.service;

import java.util.regex.Matcher;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.sresthaa.publicui.dto.BlogSummary;

// Splices real per-post meta tags into ui's shell HTML, for crawlers that never execute JS.
// Same shell serves real browsers too (harmless), so no user-agent sniffing needed.
@Service
public class BlogPreviewService {

	public String withPostMeta(String shellHtml, BlogSummary post, String pageUrl) {
		String title = post.title();
		String description = post.excerpt();
		String image = post.coverImage();
		boolean hasImage = image != null && !image.isBlank();

		StringBuilder metaTags = new StringBuilder();
		metaTags.append("<meta name=\"description\" content=\"").append(escapeHtml(description)).append("\">\n    ");
		metaTags.append("<meta property=\"og:type\" content=\"article\">\n    ");
		metaTags.append("<meta property=\"og:title\" content=\"").append(escapeHtml(title)).append("\">\n    ");
		metaTags.append("<meta property=\"og:description\" content=\"")
				.append(escapeHtml(description))
				.append("\">\n    ");
		metaTags.append("<meta property=\"og:url\" content=\"").append(escapeHtml(pageUrl)).append("\">\n    ");
		if (hasImage) {
			metaTags.append("<meta property=\"og:image\" content=\"").append(escapeHtml(image)).append("\">\n    ");
		}
		metaTags.append("<meta name=\"twitter:card\" content=\"")
				.append(hasImage ? "summary_large_image" : "summary")
				.append("\">");

		return shellHtml
				.replaceFirst("<title>.*?</title>", Matcher.quoteReplacement("<title>" + escapeHtml(title) + "</title>"))
				.replace("</head>", "    " + metaTags + "\n  </head>");
	}

	private String escapeHtml(String value) {
		return value == null ? "" : HtmlUtils.htmlEscape(value);
	}
}
