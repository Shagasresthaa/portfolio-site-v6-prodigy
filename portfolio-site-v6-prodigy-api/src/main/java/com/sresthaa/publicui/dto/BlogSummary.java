package com.sresthaa.publicui.dto;

import java.time.Instant;
import java.util.UUID;

import com.sresthaa.publicui.model.Blog;

public record BlogSummary(UUID id, String slug, String title, String excerpt, String content, String coverImage,
		boolean published, Instant publishedAt, String tags, int likeCount, int dislikeCount) {

	public static BlogSummary from(Blog blog) {
		return new BlogSummary(blog.getId(), blog.getSlug(), blog.getTitle(), blog.getExcerpt(), blog.getContent(),
				blog.getCoverImage(), blog.isPublished(), blog.getPublishedAt(), blog.getTags(), blog.getLikeCount(),
				blog.getDislikeCount());
	}
}
