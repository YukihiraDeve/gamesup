package com.gamesup.api.auth.web.dto;

import static com.gamesup.api.common.web.validation.ValidationRules.EMAIL_MAX_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PASSWORD_MAX_LENGTH;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
		@Schema(example = "alice@example.com")
		@NotBlank @Email @Size(max = EMAIL_MAX_LENGTH) String email,
		@Schema(example = "A-strong-password-2026", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
		@NotBlank @Size(max = PASSWORD_MAX_LENGTH) String password) {
}
