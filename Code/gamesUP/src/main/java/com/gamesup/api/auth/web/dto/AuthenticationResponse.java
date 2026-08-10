package com.gamesup.api.auth.web.dto;

import com.gamesup.api.auth.domain.Role;

public record AuthenticationResponse(
		String accessToken,
		String tokenType,
		long expiresInSeconds,
		Long userId,
		String email,
		Role role) {
}
