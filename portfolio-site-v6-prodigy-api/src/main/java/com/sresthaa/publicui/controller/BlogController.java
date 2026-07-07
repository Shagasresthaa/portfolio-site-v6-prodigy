package com.sresthaa.publicui.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.publicui.dto.BlogCommentRequest;
import com.sresthaa.publicui.dto.BlogCommentSummary;
import com.sresthaa.publicui.dto.BlogReactionCounts;
import com.sresthaa.publicui.dto.BlogReactionRequest;
import com.sresthaa.publicui.dto.BlogSummary;
import com.sresthaa.publicui.dto.BlogUpsertRequest;
import com.sresthaa.publicui.service.BlogService;

@RestController
public class BlogController {

	private final BlogService blogService;

	public BlogController(BlogService blogService) {
		this.blogService = blogService;
	}

	@GetMapping("/api/blog")
	public List<BlogSummary> listPublished() {
		return blogService.listPublished();
	}

	// 404s (not just omits) an unpublished/nonexistent slug - matches the current UI's
	// treatment of an unpublished post as "not found", not "forbidden".
	@GetMapping("/api/blog/{slug}")
	public BlogSummary getBySlug(@PathVariable String slug) {
		return blogService.getPublishedBySlug(slug);
	}

	@GetMapping("/api/blog/{slug}/comments")
	public List<BlogCommentSummary> listComments(@PathVariable String slug) {
		return blogService.listComments(slug);
	}

	@PostMapping("/api/blog/{slug}/comments")
	public BlogCommentSummary addComment(@PathVariable String slug, @RequestBody BlogCommentRequest request) {
		return blogService.addComment(slug, request);
	}

	@PostMapping("/api/blog/{slug}/reactions")
	public BlogReactionCounts applyReaction(@PathVariable String slug, @RequestBody BlogReactionRequest request) {
		return blogService.applyReaction(slug, request);
	}

	// Admin sees every post (drafts included) - the public endpoints above always filter to
	// published only.
	@GetMapping("/api/admin/blog")
	public List<BlogSummary> listAll() {
		return blogService.listAll();
	}

	@PostMapping("/api/admin/blog")
	public BlogSummary create(@RequestBody BlogUpsertRequest request) {
		return blogService.create(request);
	}

	@PutMapping("/api/admin/blog/{id}")
	public BlogSummary update(@PathVariable UUID id, @RequestBody BlogUpsertRequest request) {
		return blogService.update(id, request);
	}

	@DeleteMapping("/api/admin/blog/{id}")
	public void delete(@PathVariable UUID id) {
		blogService.delete(id);
	}
}
