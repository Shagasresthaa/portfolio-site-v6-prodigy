package com.sresthaa.publicui.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A reusable image library for blog post content - upload once, then reference the same URL
// across multiple posts' markdown, rather than re-uploading per post.
@Entity
@Table(name = "blog_image")
@EntityListeners(AuditingEntityListener.class)
public class BlogImage {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String url;

	@Column(name = "alt_text")
	private String altText;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected BlogImage() {
	}

	public BlogImage(String url, String altText) {
		this.url = url;
		this.altText = altText;
	}

	public UUID getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getAltText() {
		return altText;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
