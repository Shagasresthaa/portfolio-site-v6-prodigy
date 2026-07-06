package com.sresthaa.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.dto.WebAuthnCredentialSummary;
import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.webauthn.WebAuthnService;

@RestController
@RequestMapping("/api/admin/webauthn")
public class WebAuthnController {

	private final AdminAccountRepository adminAccountRepository;
	private final WebAuthnService webAuthnService;

	public WebAuthnController(AdminAccountRepository adminAccountRepository, WebAuthnService webAuthnService) {
		this.adminAccountRepository = adminAccountRepository;
		this.webAuthnService = webAuthnService;
	}

	@PostMapping(value = "/register/options", produces = MediaType.APPLICATION_JSON_VALUE)
	public String beginRegistration(Authentication authentication) {
		return webAuthnService.beginRegistration(currentAccount(authentication));
	}

	// Raw registrationResponseJSON body, produced by navigator.credentials.create() client-side.
	// Caller must send Content-Type: text/plain - the body is JSON text, but binding it into a
	// String param (rather than parsing it as a JSON object) needs Spring's plain-text converter.
	@PostMapping(value = "/register/verify", consumes = MediaType.TEXT_PLAIN_VALUE)
	public void verifyRegistration(Authentication authentication, @RequestParam String label,
			@RequestBody String registrationResponseJSON) {
		webAuthnService.verifyRegistration(currentAccount(authentication), registrationResponseJSON, label);
	}

	@GetMapping("/credentials")
	public List<WebAuthnCredentialSummary> listCredentials(Authentication authentication) {
		return webAuthnService.listCredentials(currentAccount(authentication));
	}

	@DeleteMapping("/credentials/{id}")
	public void deleteCredential(Authentication authentication, @PathVariable UUID id) {
		webAuthnService.deleteCredential(currentAccount(authentication), id);
	}

	private AdminAccount currentAccount(Authentication authentication) {
		return adminAccountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}
}
