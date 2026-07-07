package com.sresthaa.publicui.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blog")
public class Blog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String slug;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String excerpt;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "cover_image")
	private String coverImage;

	@Column(nullable = false)
	private boolean published;

	@Column(name = "published_at")
	private Instant publishedAt;

	// Comma-separated, matching the old site's storage format (see src/types/blog.ts on both
	// frontends).
	@Column(nullable = false)
	private String tags;

	@Column(name = "like_count", nullable = false)
	private int likeCount;

	@Column(name = "dislike_count", nullable = false)
	private int dislikeCount;

	protected Blog() {
	}

	public Blog(String slug, String title, String excerpt, String content, String coverImage, boolean published,
			Instant publishedAt, String tags, int likeCount, int dislikeCount) {
		this.slug = slug;
		this.title = title;
		this.excerpt = excerpt;
		this.content = content;
		this.coverImage = coverImage;
		this.published = published;
		this.publishedAt = publishedAt;
		this.tags = tags;
		this.likeCount = likeCount;
		this.dislikeCount = dislikeCount;
	}

	public UUID getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getExcerpt() {
		return excerpt;
	}

	public void setExcerpt(String excerpt) {
		this.excerpt = excerpt;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getCoverImage() {
		return coverImage;
	}

	public void setCoverImage(String coverImage) {
		this.coverImage = coverImage;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(Instant publishedAt) {
		this.publishedAt = publishedAt;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public int getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(int likeCount) {
		this.likeCount = likeCount;
	}

	public int getDislikeCount() {
		return dislikeCount;
	}

	public void setDislikeCount(int dislikeCount) {
		this.dislikeCount = dislikeCount;
	}
}
