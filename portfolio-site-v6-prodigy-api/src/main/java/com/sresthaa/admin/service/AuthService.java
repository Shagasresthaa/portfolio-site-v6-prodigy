package com.sresthaa.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.model.SystemOverrideKeys;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.repository.SystemOverrideRepository;
import com.sresthaa.admin.security.JwtService;

@Service
public class AuthService {

	private final AdminAccountRepository adminAccountRepository;
	private final SystemOverrideRepository systemOverrideRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(AdminAccountRepository adminAccountRepository,
			SystemOverrideRepository systemOverrideRepository, PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.adminAccountRepository = adminAccountRepository;
		this.systemOverrideRepository = systemOverrideRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public String login(String username, String rawPassword) {
		AdminAccount account = adminAccountRepository.findByUsername(username)
				.filter(a -> passwordEncoder.matches(rawPassword, a.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (!bypassSecondFactorEnabled()) {
			throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Second-factor login not yet implemented");
		}

		return jwtService.issueToken(account.getUsername());
	}

	private boolean bypassSecondFactorEnabled() {
		return systemOverrideRepository.findById(SystemOverrideKeys.BYPASS_SECOND_FACTOR)
				.map(override -> Boolean.parseBoolean(override.getValue()))
				.orElse(false);
	}
}
