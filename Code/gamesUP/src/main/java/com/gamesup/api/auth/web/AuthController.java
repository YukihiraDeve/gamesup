package com.gamesup.api.auth.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.auth.application.AuthService;
import com.gamesup.api.auth.web.dto.AuthenticationResponse;
import com.gamesup.api.auth.web.dto.LoginRequest;
import com.gamesup.api.auth.web.dto.RegisterRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public AuthenticationResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
