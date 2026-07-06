package com.sresthaa.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.dto.LoginResponse;
import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.security.JwtService;

@Service
public class AccountService {

	private static final int MIN_PASSWORD_LENGTH = 8;

	private final AdminAccountRepository adminAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AccountService(AdminAccountRepository adminAccountRepository, PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.adminAccountRepository = adminAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	// Re-issues a token because the old one remains cryptographically valid until it naturally
	// expires - the password change itself doesn't revoke it. That's an accepted gap for a
	// single-admin, single-session tool; a real token-blocklist would be overkill here.
	public LoginResponse changePassword(AdminAccount account, String currentPassword, String newPassword) {
		requireCurrentPassword(account, currentPassword);

		if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}

		account.setPasswordHash(passwordEncoder.encode(newPassword));
		adminAccountRepository.save(account);

		return LoginResponse.issued(jwtService.issueToken(account.getUsername()));
	}

	// Must re-issue a token here (not optional, unlike changePassword): the previous token's
	// subject is the old username, which no longer resolves to any account once renamed.
	public LoginResponse changeUsername(AdminAccount account, String currentPassword, String newUsername) {
		requireCurrentPassword(account, currentPassword);

		String trimmed = newUsername == null ? "" : newUsername.trim();
		if (trimmed.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
		}
		if (!trimmed.equals(account.getUsername()) && adminAccountRepository.findByUsername(trimmed).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
		}

		account.setUsername(trimmed);
		adminAccountRepository.save(account);

		return LoginResponse.issued(jwtService.issueToken(trimmed));
	}

	public void verifyPassword(AdminAccount account, String password) {
		requireCurrentPassword(account, password);
	}

	private void requireCurrentPassword(AdminAccount account, String password) {
		if (!passwordEncoder.matches(password == null ? "" : password, account.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
		}
	}
}
