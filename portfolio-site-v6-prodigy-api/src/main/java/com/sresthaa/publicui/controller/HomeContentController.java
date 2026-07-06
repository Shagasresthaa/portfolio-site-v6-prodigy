package com.sresthaa.publicui.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.publicui.dto.HomeContentPayload;
import com.sresthaa.publicui.service.HomeContentService;

@RestController
public class HomeContentController {

	private final HomeContentService homeContentService;

	public HomeContentController(HomeContentService homeContentService) {
		this.homeContentService = homeContentService;
	}

	@GetMapping("/api/home")
	public HomeContentPayload get() {
		return homeContentService.get();
	}

	@PutMapping("/api/admin/home")
	public HomeContentPayload update(@RequestBody HomeContentPayload payload) {
		return homeContentService.update(payload);
	}
}
