package com.sresthaa.publicui.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.publicui.service.BlogPreviewService;
import com.sresthaa.publicui.service.BlogService;
import com.sresthaa.publicui.service.UiShellClient;

import jakarta.servlet.http.HttpServletRequest;

// k8s/ingress.yaml routes /blog and /blog/* on the public hostnames here instead of to the ui
// service - everything else (static assets, in-app client-side navigation once the SPA has
// booted) still goes straight to ui, untouched. See BlogPreviewService for why this exists at
// all: link-preview crawlers never execute JS, so without this every shared blog post URL would
// show the same generic, content-less preview regardless of which post it actually is.
@RestController
public class BlogPreviewController {

	private final BlogService blogService;
	private final BlogPreviewService blogPreviewService;
	private final UiShellClient uiShellClient;

	public BlogPreviewController(BlogService blogService, BlogPreviewService blogPreviewService,
			UiShellClient uiShellClient) {
		this.blogService = blogService;
		this.blogPreviewService = blogPreviewService;
		this.uiShellClient = uiShellClient;
	}

	// The list page has no single entity to describe - just pass the shell through untouched,
	// same as before this controller existed.
	@GetMapping(value = "/blog", produces = MediaType.TEXT_HTML_VALUE)
	public String blogList() {
		return uiShellClient.fetchIndexHtml();
	}

	@GetMapping(value = "/blog/{slug}", produces = MediaType.TEXT_HTML_VALUE)
	public String blogPost(@PathVariable String slug, HttpServletRequest request) {
		String shell = uiShellClient.fetchIndexHtml();

		return blogService.findPublishedForPreview(slug)
				.map(post -> blogPreviewService.withPostMeta(shell, post, pageUrl(request, slug)))
				.orElse(shell);
	}

	private String pageUrl(HttpServletRequest request, String slug) {
		String proto = request.getHeader("X-Forwarded-Proto");
		if (proto == null) {
			proto = request.getScheme();
		}
		String host = request.getHeader("X-Forwarded-Host");
		if (host == null) {
			host = request.getHeader("Host");
		}
		return proto + "://" + host + "/blog/" + slug;
	}
}
