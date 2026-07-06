package com.sresthaa.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sresthaa.admin.dto.TotpEnrollmentOptions;
import com.sresthaa.admin.dto.TotpStatus;
import com.sresthaa.admin.model.AdminAccount;
import com.sresthaa.admin.repository.AdminAccountRepository;
import com.sresthaa.admin.totp.TotpService;

@RestController
@RequestMapping("/api/admin/totp")
public class TotpController {

	private final AdminAccountRepository adminAccountRepository;
	private final TotpService totpService;

	public TotpController(AdminAccountRepository adminAccountRepository, TotpService totpService) {
		this.adminAccountRepository = adminAccountRepository;
		this.totpService = totpService;
	}

	@GetMapping("/status")
	public TotpStatus status(Authentication authentication) {
		return totpService.status(currentAccount(authentication));
	}

	@PostMapping("/enroll/begin")
	public TotpEnrollmentOptions beginEnrollment(Authentication authentication) {
		return totpService.beginEnrollment(currentAccount(authentication));
	}

	@PostMapping("/enroll/confirm")
	public List<String> confirmEnrollment(Authentication authentication, @RequestParam String code) {
		return totpService.confirmEnrollment(currentAccount(authentication), code);
	}

	@PostMapping("/backup-codes/regenerate")
	public List<String> regenerateBackupCodes(Authentication authentication) {
		return totpService.regenerateBackupCodes(currentAccount(authentication));
	}

	@DeleteMapping
	public void disable(Authentication authentication) {
		totpService.disable(currentAccount(authentication));
	}

	private AdminAccount currentAccount(Authentication authentication) {
		return adminAccountRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}
}
