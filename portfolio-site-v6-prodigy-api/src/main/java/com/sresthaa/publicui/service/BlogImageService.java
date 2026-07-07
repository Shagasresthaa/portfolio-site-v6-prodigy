package com.sresthaa.publicui.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.publicui.dto.BlogImageCreateRequest;
import com.sresthaa.publicui.dto.BlogImageSummary;
import com.sresthaa.publicui.model.BlogImage;
import com.sresthaa.publicui.repository.BlogImageRepository;
import com.sresthaa.storage.R2StorageService;

@Service
public class BlogImageService {

	private static final Logger log = LoggerFactory.getLogger(BlogImageService.class);

	private final BlogImageRepository blogImageRepository;
	private final R2StorageService r2StorageService;

	public BlogImageService(BlogImageRepository blogImageRepository, R2StorageService r2StorageService) {
		this.blogImageRepository = blogImageRepository;
		this.r2StorageService = r2StorageService;
	}

	public List<BlogImageSummary> listAll() {
		return blogImageRepository.findAllByOrderByCreatedAtDesc().stream().map(BlogImageSummary::from).toList();
	}

	public BlogImageSummary create(BlogImageCreateRequest request) {
		String url = request.url() == null ? "" : request.url().trim();
		if (url.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL is required");
		}

		String altText = request.altText() == null ? "" : request.altText().trim();
		BlogImage image = new BlogImage(url, altText.isEmpty() ? null : altText);
		return BlogImageSummary.from(blogImageRepository.save(image));
	}

	public void delete(UUID id) {
		BlogImage image = blogImageRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such image"));

		blogImageRepository.deleteById(id);

		// Best-effort: an R2 hiccup here shouldn't fail the delete the admin already confirmed -
		// worst case is a harmless orphaned object, logged for manual cleanup.
		try {
			r2StorageService.delete(image.getUrl());
		} catch (RuntimeException e) {
			log.warn("Failed to delete R2 object at {}", image.getUrl(), e);
		}
	}
}
