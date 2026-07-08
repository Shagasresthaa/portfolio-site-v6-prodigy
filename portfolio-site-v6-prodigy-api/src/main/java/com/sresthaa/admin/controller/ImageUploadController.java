package com.sresthaa.admin.controller;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.dto.UploadResponse;
import com.sresthaa.storage.R2StorageService;

@RestController
@RequestMapping("/api/admin/uploads")
public class ImageUploadController {

	// Bucket key prefix, one per content type that uploads images - keeps R2 from becoming one
	// flat pile of files. Add to this set as more content types gain real image uploads.
	private static final Set<String> ALLOWED_CATEGORIES = Set.of("highlights", "projects", "blog", "settings", "certs");

	private static final Set<String> ALLOWED_DOCUMENT_CATEGORIES = Set.of("resume");

	private final R2StorageService r2StorageService;

	public ImageUploadController(R2StorageService r2StorageService) {
		this.r2StorageService = r2StorageService;
	}

	@PostMapping("/image")
	public UploadResponse uploadImage(@RequestParam("file") MultipartFile file,
			@RequestParam("category") String category) {
		if (!ALLOWED_CATEGORIES.contains(category)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown upload category");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an image");
		}

		try {
			String url = r2StorageService.upload(file.getBytes(), contentType, category);
			return new UploadResponse(url);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file", e);
		}
	}

	@PostMapping("/document")
	public UploadResponse uploadDocument(@RequestParam("file") MultipartFile file,
			@RequestParam("category") String category) {
		if (!ALLOWED_DOCUMENT_CATEGORIES.contains(category)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown upload category");
		}

		String contentType = file.getContentType();
		if (!"application/pdf".equals(contentType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be a PDF");
		}

		try {
			String url = r2StorageService.upload(file.getBytes(), contentType, category);
			return new UploadResponse(url);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file", e);
		}
	}
}
