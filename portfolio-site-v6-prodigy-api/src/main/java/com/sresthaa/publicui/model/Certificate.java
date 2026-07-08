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

// Certification/award images shown in the public site's floating certificates carousel (see
// CertificatesCarousel.vue) - always public, no draft/published distinction, same as Highlight.
@Entity
@Table(name = "certificate")
@EntityListeners(AuditingEntityListener.class)
public class Certificate {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	private String title;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Certificate() {
	}

	public Certificate(String imageUrl, String title) {
		this.imageUrl = imageUrl;
		this.title = title;
	}

	public UUID getId() {
		return id;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getTitle() {
		return title;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
