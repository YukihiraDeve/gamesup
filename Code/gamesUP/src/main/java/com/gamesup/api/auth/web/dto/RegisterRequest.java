package com.gamesup.api.auth.web.dto;

import static com.gamesup.api.common.web.validation.ValidationRules.EMAIL_MAX_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PASSWORD_MAX_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PASSWORD_MIN_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PERSON_NAME_MAX_LENGTH;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterRequest(
		@Schema(example = "alice@example.com")
		@NotBlank @Email @Size(max = EMAIL_MAX_LENGTH) String email,
		@Schema(example = "A-strong-password-2026", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
		@NotBlank @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH) String password,
		@Schema(example = "Alice")
		@NotBlank @Size(max = PERSON_NAME_MAX_LENGTH) String firstName,
		@Schema(example = "Martin")
		@NotBlank @Size(max = PERSON_NAME_MAX_LENGTH) String lastName) {
}
