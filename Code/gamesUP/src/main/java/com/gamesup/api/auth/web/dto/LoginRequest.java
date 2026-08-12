package com.gamesup.api.auth.web.dto;

import static com.gamesup.api.common.web.validation.ValidationRules.EMAIL_MAX_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PASSWORD_MAX_LENGTH;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Email @Size(max = EMAIL_MAX_LENGTH) String email,
		@NotBlank @Size(max = PASSWORD_MAX_LENGTH) String password) {
}
