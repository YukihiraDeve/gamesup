package com.gamesup.api.customer.web;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.config.web.OpenApiConfiguration;
import com.gamesup.api.customer.application.UserProfileService;
import com.gamesup.api.customer.web.dto.UserProfileUpdateRequest;
import com.gamesup.api.customer.web.dto.UserResponse;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/users/me")
@Tag(
		name = "Current user",
		description = "Profil déterminé uniquement par le JWT ; aucun identifiant utilisateur n'est accepté.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class UserProfileController {

	private final UserProfileService userProfileService;

	public UserProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping
	@Operation(summary = "Consulter son profil")
	public UserResponse findCurrentUser(@AuthenticationPrincipal GamesUpUserPrincipal principal) {
		return userProfileService.findCurrentUser(principal.userId());
	}

	@PatchMapping
	@Operation(summary = "Modifier partiellement son profil")
	public UserResponse updateCurrentUser(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@Valid @RequestBody UserProfileUpdateRequest request) {
		return userProfileService.updateCurrentUser(principal.userId(), request);
	}
}
