package com.sresthaa.publicui.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "blog_comment")
@EntityListeners(AuditingEntityListener.class)
public class BlogComment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "blog_id", nullable = false)
	private Blog blog;

	@Column
	private String name;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected BlogComment() {
	}

	public BlogComment(Blog blog, String name, String content) {
		this.blog = blog;
		this.name = name;
		this.content = content;
	}

	public UUID getId() {
		return id;
	}

	public Blog getBlog() {
		return blog;
	}

	public String getName() {
		return name;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
