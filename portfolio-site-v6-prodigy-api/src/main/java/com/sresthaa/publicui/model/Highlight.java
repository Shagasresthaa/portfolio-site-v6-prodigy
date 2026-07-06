package com.sresthaa.publicui.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "highlight")
@EntityListeners(AuditingEntityListener.class)
public class Highlight {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column
	private String caption;

	@Enumerated(EnumType.STRING)
	@Column(name = "media_type", nullable = false)
	private HighlightMediaType mediaType;

	@Column
	private String image;

	@Column(name = "thumbnail_image")
	private String thumbnailImage;

	@Column(name = "video_url")
	private String videoUrl;

	// Comma-separated, matching the old site's storage format (see src/types/highlight.ts on
	// both frontends).
	@Column(nullable = false)
	private String tags;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Highlight() {
	}

	public Highlight(String title, String description, String caption, HighlightMediaType mediaType, String image,
			String thumbnailImage, String videoUrl, String tags) {
		this.title = title;
		this.description = description;
		this.caption = caption;
		this.mediaType = mediaType;
		this.image = image;
		this.thumbnailImage = thumbnailImage;
		this.videoUrl = videoUrl;
		this.tags = tags;
	}

	public UUID getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public HighlightMediaType getMediaType() {
		return mediaType;
	}

	public void setMediaType(HighlightMediaType mediaType) {
		this.mediaType = mediaType;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getThumbnailImage() {
		return thumbnailImage;
	}

	public void setThumbnailImage(String thumbnailImage) {
		this.thumbnailImage = thumbnailImage;
	}

	public String getVideoUrl() {
		return videoUrl;
	}

	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
