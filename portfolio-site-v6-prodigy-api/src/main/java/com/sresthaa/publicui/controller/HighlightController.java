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

import com.sresthaa.publicui.dto.HighlightSummary;
import com.sresthaa.publicui.dto.HighlightUpsertRequest;
import com.sresthaa.publicui.service.HighlightService;

@RestController
public class HighlightController {

	private final HighlightService highlightService;

	public HighlightController(HighlightService highlightService) {
		this.highlightService = highlightService;
	}

	// Every highlight is always public - no draft/published distinction (unlike blog posts) -
	// so this same endpoint serves both the public site and the admin manager.
	@GetMapping("/api/highlights")
	public List<HighlightSummary> listHighlights() {
		return highlightService.listAll();
	}

	@PostMapping("/api/admin/highlights")
	public HighlightSummary create(@RequestBody HighlightUpsertRequest request) {
		return highlightService.create(request);
	}

	@PutMapping("/api/admin/highlights/{id}")
	public HighlightSummary update(@PathVariable UUID id, @RequestBody HighlightUpsertRequest request) {
		return highlightService.update(id, request);
	}

	@DeleteMapping("/api/admin/highlights/{id}")
	public void delete(@PathVariable UUID id) {
		highlightService.delete(id);
	}
}
