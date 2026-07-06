package com.sresthaa.admin.model;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "totp_backup_code")
@EntityListeners(AuditingEntityListener.class)
public class TotpBackupCode {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "totp_credential_id", nullable = false)
	private TotpCredential totpCredential;

	// One-way hash of the backup code — never store or log the raw code
	@Column(name = "code_hash", nullable = false)
	private String codeHash;

	@Column(nullable = false)
	private boolean used;

	@Column(name = "used_at")
	private Instant usedAt;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected TotpBackupCode() {
	}

	public TotpBackupCode(TotpCredential totpCredential, String codeHash) {
		this.totpCredential = totpCredential;
		this.codeHash = codeHash;
		this.used = false;
	}

	public UUID getId() {
		return id;
	}

	public TotpCredential getTotpCredential() {
		return totpCredential;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public boolean isUsed() {
		return used;
	}

	public void markUsed(Instant when) {
		this.used = true;
		this.usedAt = when;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
