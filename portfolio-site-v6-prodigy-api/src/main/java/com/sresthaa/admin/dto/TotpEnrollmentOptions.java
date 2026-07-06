package com.sresthaa.admin.dto;

// qrCodeImagePng is base64-encoded PNG bytes - secret/otpauthUri are also included so the user
// can enter the key manually if they can't scan a QR code (e.g. entering it on the same device).
public record TotpEnrollmentOptions(String secret, String otpauthUri, String qrCodeImagePng) {
}
