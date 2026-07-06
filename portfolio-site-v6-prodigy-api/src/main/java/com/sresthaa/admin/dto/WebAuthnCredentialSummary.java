package com.sresthaa.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.sresthaa.admin.model.WebAuthnCredential;

public record WebAuthnCredentialSummary(UUID id, String label, Instant createdAt, Instant lastUsedAt) {

	public static WebAuthnCredentialSummary from(WebAuthnCredential credential) {
		return new WebAuthnCredentialSummary(credential.getId(), credential.getLabel(), credential.getCreatedAt(),
				credential.getLastUsedAt());
	}
}
