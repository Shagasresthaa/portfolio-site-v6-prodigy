package com.sresthaa.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.dto.LoginResponse;
import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.model.SystemOverrideKeys;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.repository.SystemOverrideRepository;
import com.sresthaa.admin.security.JwtService;
import com.sresthaa.admin.webauthn.WebAuthnService;

@Service
public class AuthService {

	private final AdminAccountRepository adminAccountRepository;
	private final SystemOverrideRepository systemOverrideRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final WebAuthnService webAuthnService;

	public AuthService(AdminAccountRepository adminAccountRepository,
			SystemOverrideRepository systemOverrideRepository, PasswordEncoder passwordEncoder,
			JwtService jwtService, WebAuthnService webAuthnService) {
		this.adminAccountRepository = adminAccountRepository;
		this.systemOverrideRepository = systemOverrideRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.webAuthnService = webAuthnService;
	}

	public LoginResponse login(String username, String rawPassword) {
		AdminAccount account = adminAccountRepository.findByUsername(username)
				.filter(a -> passwordEncoder.matches(rawPassword, a.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (bypassSecondFactorEnabled()) {
			return LoginResponse.issued(jwtService.issueToken(account.getUsername()));
		}

		if (webAuthnService.hasCredentials(account)) {
			return LoginResponse.webAuthnRequired(webAuthnService.beginAuthentication(account));
		}

		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Second-factor login not yet implemented");
	}

	public LoginResponse completeWebAuthnLogin(String username, String authenticationResponseJSON) {
		AdminAccount account = adminAccountRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		webAuthnService.verifyAuthentication(account, authenticationResponseJSON);

		return LoginResponse.issued(jwtService.issueToken(account.getUsername()));
	}

	private boolean bypassSecondFactorEnabled() {
		return systemOverrideRepository.findById(SystemOverrideKeys.BYPASS_SECOND_FACTOR)
				.map(override -> Boolean.parseBoolean(override.getValue()))
				.orElse(false);
	}
}
