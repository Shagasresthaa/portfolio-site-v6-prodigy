package com.sresthaa.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.dto.ChangePasswordRequest;
import com.sresthaa.admin.dto.ChangeUsernameRequest;
import com.sresthaa.admin.dto.LoginResponse;
import com.sresthaa.admin.dto.MeResponse;
import com.sresthaa.admin.dto.VerifyPasswordRequest;
import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.service.AccountService;

@RestController
@RequestMapping("/api/admin/account")
public class AccountController {

	private final AdminAccountRepository adminAccountRepository;
	private final AccountService accountService;

	public AccountController(AdminAccountRepository adminAccountRepository, AccountService accountService) {
		this.adminAccountRepository = adminAccountRepository;
		this.accountService = accountService;
	}

	// Lets the frontend confirm a stored token is still genuinely valid (and fetch the
	// canonical username) instead of trusting client-side state alone - see AuthGate.vue.
	@GetMapping("/me")
	public MeResponse me(Authentication authentication) {
		return new MeResponse(currentAccount(authentication).getUsername());
	}

	@PostMapping("/password")
	public LoginResponse changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
		return accountService.changePassword(currentAccount(authentication), request.currentPassword(),
				request.newPassword());
	}

	@PostMapping("/username")
	public LoginResponse changeUsername(Authentication authentication, @RequestBody ChangeUsernameRequest request) {
		return accountService.changeUsername(currentAccount(authentication), request.currentPassword(),
				request.newUsername());
	}

	@PostMapping("/verify-password")
	public void verifyPassword(Authentication authentication, @RequestBody VerifyPasswordRequest request) {
		accountService.verifyPassword(currentAccount(authentication), request.password());
	}

	private AdminAccount currentAccount(Authentication authentication) {
		return adminAccountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}
}
