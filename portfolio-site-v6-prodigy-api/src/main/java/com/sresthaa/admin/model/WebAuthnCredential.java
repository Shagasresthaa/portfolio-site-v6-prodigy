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
@Table(name = "webauthn_credential")
@EntityListeners(AuditingEntityListener.class)
public class WebAuthnCredential {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "admin_account_id", nullable = false)
	private AdminAccount adminAccount;

	// Identifies the authenticator model, not a secret - needed to reconstruct AttestedCredentialData for verification
	@Column(nullable = false)
	private byte[] aaguid;

	@Column(name = "credential_id", nullable = false, unique = true)
	private byte[] credentialId;

	@Column(name = "public_key_cose", nullable = false)
	private byte[] publicKeyCose;

	// Must strictly increase on every successful assertion (clone detection)
	@Column(name = "signature_count", nullable = false)
	private long signatureCount;

	// Comma-separated, e.g. "usb,nfc"
	@Column(name = "transports")
	private String transports;

	@Column(nullable = false)
	private String label;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	protected WebAuthnCredential() {
	}

	public WebAuthnCredential(AdminAccount adminAccount, byte[] aaguid, byte[] credentialId, byte[] publicKeyCose,
			long signatureCount, String transports, String label) {
		this.adminAccount = adminAccount;
		this.aaguid = aaguid;
		this.credentialId = credentialId;
		this.publicKeyCose = publicKeyCose;
		this.signatureCount = signatureCount;
		this.transports = transports;
		this.label = label;
	}

	public UUID getId() {
		return id;
	}

	public AdminAccount getAdminAccount() {
		return adminAccount;
	}

	public byte[] getAaguid() {
		return aaguid;
	}

	public byte[] getCredentialId() {
		return credentialId;
	}

	public byte[] getPublicKeyCose() {
		return publicKeyCose;
	}

	public long getSignatureCount() {
		return signatureCount;
	}

	public void setSignatureCount(long signatureCount) {
		this.signatureCount = signatureCount;
	}

	public String getTransports() {
		return transports;
	}

	public void setTransports(String transports) {
		this.transports = transports;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}
}
