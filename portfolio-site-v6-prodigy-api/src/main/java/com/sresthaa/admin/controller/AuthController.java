package com.sresthaa.admin.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sresthaa.admin.dto.LoginRequest;
import com.sresthaa.admin.dto.LoginResponse;
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
		return new LoginResponse(authService.login(request.username(), request.password()));
	}
}
