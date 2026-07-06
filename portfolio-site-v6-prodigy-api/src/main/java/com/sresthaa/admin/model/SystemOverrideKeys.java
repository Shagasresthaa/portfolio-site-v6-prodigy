package com.sresthaa.admin.model;

public final class SystemOverrideKeys {

	// When "true", login succeeds on password alone, skipping WebAuthn/TOTP entirely.
	public static final String BYPASS_SECOND_FACTOR = "auth.bypass_second_factor";

	private SystemOverrideKeys() {
	}
}
