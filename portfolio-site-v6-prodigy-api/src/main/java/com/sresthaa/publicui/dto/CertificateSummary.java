package com.sresthaa.publicui.dto;

import java.time.Instant;
import java.util.UUID;

import com.sresthaa.publicui.model.Certificate;

public record CertificateSummary(UUID id, String imageUrl, String title, Instant createdAt) {

	public static CertificateSummary from(Certificate certificate) {
		return new CertificateSummary(certificate.getId(), certificate.getImageUrl(), certificate.getTitle(),
				certificate.getCreatedAt());
	}
}
