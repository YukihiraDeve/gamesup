package com.gamesup.api.auth.web.dto;

import static com.gamesup.api.common.web.validation.ValidationRules.EMAIL_MAX_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PASSWORD_MAX_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PASSWORD_MIN_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PERSON_NAME_MAX_LENGTH;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = EMAIL_MAX_LENGTH) String email,
		@NotBlank @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH) String password,
		@NotBlank @Size(max = PERSON_NAME_MAX_LENGTH) String firstName,
		@NotBlank @Size(max = PERSON_NAME_MAX_LENGTH) String lastName) {
}
