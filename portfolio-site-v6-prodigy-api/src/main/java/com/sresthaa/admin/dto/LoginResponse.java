package com.sresthaa.admin.dto;

import java.util.List;

public record LoginResponse(String status, String token, String webAuthnOptions, List<String> availableMethods) {

	public static LoginResponse issued(String token) {
		return new LoginResponse("OK", token, null, null);
	}

	public static LoginResponse webAuthnRequired(String options) {
		return new LoginResponse("WEBAUTHN_REQUIRED", null, options, null);
	}

	public static LoginResponse totpRequired() {
		return new LoginResponse("TOTP_REQUIRED", null, null, null);
	}

	// webAuthnOptions is pre-fetched here (not just on WEBAUTHN_REQUIRED) so picking "security key"
	// doesn't need a second round trip to start the ceremony.
	public static LoginResponse secondFactorChoiceRequired(String webAuthnOptions) {
		return new LoginResponse("SECOND_FACTOR_CHOICE_REQUIRED", null, webAuthnOptions,
				List.of("WEBAUTHN", "TOTP"));
	}
}
