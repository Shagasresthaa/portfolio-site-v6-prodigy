package com.sresthaa.admin.dto;

public record LoginResponse(String status, String token, String webAuthnOptions) {

	public static LoginResponse issued(String token) {
		return new LoginResponse("OK", token, null);
	}

	public static LoginResponse webAuthnRequired(String options) {
		return new LoginResponse("WEBAUTHN_REQUIRED", null, options);
	}
}
