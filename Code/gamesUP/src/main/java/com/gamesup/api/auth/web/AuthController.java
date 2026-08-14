package com.gamesup.api.auth.web;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Authentication", description = "Inscription CLIENT et obtention d'un jeton JWT.")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@Operation(
			summary = "Créer un compte client",
			description = "Crée exclusivement un compte CLIENT et retourne immédiatement un JWT.",
			responses = @ApiResponse(
					responseCode = "201",
					description = "Compte créé.",
					content = @Content(
							schema = @Schema(implementation = AuthenticationResponse.class),
							examples = @ExampleObject(value = """
									{"accessToken":"eyJ...","tokenType":"Bearer","expiresInSeconds":3600,
									"userId":42,"email":"alice@example.com","role":"CLIENT"}
									"""))))
	public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	@Operation(
			summary = "Se connecter",
			description = "Authentifie un compte actif sans révéler si l'email ou le mot de passe est incorrect.")
	public AuthenticationResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
