package com.sresthaa.publicui.dto;

import com.sresthaa.publicui.model.HighlightMediaType;

public record HighlightUpsertRequest(String title, String description, String caption,
		HighlightMediaType mediaType, String image, String thumbnailImage, String videoUrl, String tags) {
}
