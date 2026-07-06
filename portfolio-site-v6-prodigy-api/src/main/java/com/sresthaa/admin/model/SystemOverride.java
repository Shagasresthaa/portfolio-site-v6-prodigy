package com.sresthaa.admin.model;

import java.time.Instant;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Generic key/value store for operational flags and config not tied to any one entity.
@Entity
@Table(name = "system_override")
@EntityListeners(AuditingEntityListener.class)
public class SystemOverride {

	@Id
	@Column(name = "override_key")
	private String key;

	@Column(name = "override_value", nullable = false)
	private String value;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SystemOverride() {
	}

	public SystemOverride(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
