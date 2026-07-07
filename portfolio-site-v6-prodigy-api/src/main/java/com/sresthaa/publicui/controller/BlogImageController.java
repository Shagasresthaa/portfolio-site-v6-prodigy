package com.sresthaa.publicui.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.publicui.dto.BlogImageCreateRequest;
import com.sresthaa.publicui.dto.BlogImageSummary;
import com.sresthaa.publicui.service.BlogImageService;

// Admin-only, unlike BlogController's public reads - this library is an authoring aid (pick a
// URL to paste into a post's markdown), never rendered directly on the public site.
@RestController
public class BlogImageController {

	private final BlogImageService blogImageService;

	public BlogImageController(BlogImageService blogImageService) {
		this.blogImageService = blogImageService;
	}

	@GetMapping("/api/admin/blog/images")
	public List<BlogImageSummary> listAll() {
		return blogImageService.listAll();
	}

	@PostMapping("/api/admin/blog/images")
	public BlogImageSummary create(@RequestBody BlogImageCreateRequest request) {
		return blogImageService.create(request);
	}

	@DeleteMapping("/api/admin/blog/images/{id}")
	public void delete(@PathVariable UUID id) {
		blogImageService.delete(id);
	}
}
