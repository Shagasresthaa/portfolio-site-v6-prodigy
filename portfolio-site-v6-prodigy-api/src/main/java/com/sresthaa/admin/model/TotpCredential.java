package com.sresthaa.admin.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sresthaa.admin.crypto.EncryptedStringConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "totp_credential")
@EntityListeners(AuditingEntityListener.class)
public class TotpCredential {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(optional = false)
	@JoinColumn(name = "admin_account_id", nullable = false, unique = true)
	private AdminAccount adminAccount;

	// Must be decryptable to compute the current code — encrypted at rest, not hashed
	@Convert(converter = EncryptedStringConverter.class)
	@Column(nullable = false, columnDefinition = "TEXT")
	private String secret;

	@Column(nullable = false)
	private boolean enabled;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected TotpCredential() {
	}

	public TotpCredential(AdminAccount adminAccount, String secret) {
		this.adminAccount = adminAccount;
		this.secret = secret;
		this.enabled = false;
	}

	public UUID getId() {
		return id;
	}

	public AdminAccount getAdminAccount() {
		return adminAccount;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
