package com.gamesup.api.customer.web.dto;

import java.time.Instant;

import com.gamesup.api.auth.domain.Role;

public record UserResponse(
		Long id,
		String email,
		String firstName,
		String lastName,
		Role role,
		boolean enabled,
		Instant createdAt,
		Instant updatedAt) {
}
