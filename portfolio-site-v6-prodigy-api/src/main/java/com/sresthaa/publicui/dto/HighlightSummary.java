package com.sresthaa.publicui.dto;

import java.time.Instant;
import java.util.UUID;

import com.sresthaa.publicui.model.Highlight;
import com.sresthaa.publicui.model.HighlightMediaType;

public record HighlightSummary(UUID id, String title, String description, String caption,
		HighlightMediaType mediaType, String image, String thumbnailImage, String videoUrl, String tags,
		Instant createdAt) {

	public static HighlightSummary from(Highlight highlight) {
		return new HighlightSummary(highlight.getId(), highlight.getTitle(), highlight.getDescription(),
				highlight.getCaption(), highlight.getMediaType(), highlight.getImage(), highlight.getThumbnailImage(),
				highlight.getVideoUrl(), highlight.getTags(), highlight.getCreatedAt());
	}
}
