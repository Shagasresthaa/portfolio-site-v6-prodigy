package com.sresthaa.publicui.dto;

import java.time.Instant;
import java.util.UUID;

import com.sresthaa.publicui.model.BlogImage;

public record BlogImageSummary(UUID id, String url, String altText, Instant createdAt) {

	public static BlogImageSummary from(BlogImage image) {
		return new BlogImageSummary(image.getId(), image.getUrl(), image.getAltText(), image.getCreatedAt());
	}
}
