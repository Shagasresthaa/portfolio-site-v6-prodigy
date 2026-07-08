package com.sresthaa.publicui.dto;

// imageUrl is already an uploaded R2 object (via the generic /api/admin/uploads/image endpoint,
// category "certs") - this just registers it as a certificate, it doesn't upload anything itself.
public record CertificateCreateRequest(String imageUrl, String title) {
}
