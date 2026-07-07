package com.sresthaa.publicui.dto;

import java.time.Instant;
import java.util.UUID;

import com.sresthaa.publicui.model.BlogComment;

public record BlogCommentSummary(UUID id, String name, String content, Instant createdAt) {

	public static BlogCommentSummary from(BlogComment comment) {
		return new BlogCommentSummary(comment.getId(), comment.getName(), comment.getContent(), comment.getCreatedAt());
	}
}
