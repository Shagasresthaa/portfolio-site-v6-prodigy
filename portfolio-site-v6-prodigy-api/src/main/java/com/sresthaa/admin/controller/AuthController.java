package com.sresthaa.admin.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.admin.dto.LoginRequest;
import com.sresthaa.admin.dto.LoginResponse;
import com.sresthaa.admin.dto.TotpLoginRequest;
import com.sresthaa.admin.service.AuthService;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public LoginResponse login(@RequestBody LoginRequest request) {
		return authService.login(request.username(), request.password());
	}

	// Raw authenticationResponseJSON body, produced by navigator.credentials.get() client-side.
	// Caller must send Content-Type: text/plain - see WebAuthnController.verifyRegistration for why.
	@PostMapping(value = "/webauthn/verify", consumes = MediaType.TEXT_PLAIN_VALUE)
	public LoginResponse verifyWebAuthn(@RequestParam String username, @RequestBody String authenticationResponseJSON) {
		return authService.completeWebAuthnLogin(username, authenticationResponseJSON);
	}

	@PostMapping("/totp/verify")
	public LoginResponse verifyTotp(@RequestBody TotpLoginRequest request) {
		return authService.completeTotpLogin(request.username(), request.code());
	}
}
