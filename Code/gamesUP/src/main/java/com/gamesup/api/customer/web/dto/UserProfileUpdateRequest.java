package com.gamesup.api.customer.web.dto;

import static com.gamesup.api.common.web.validation.ValidationRules.EMAIL_MAX_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PERSON_NAME_MAX_LENGTH;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileUpdateRequest(
		@Schema(example = "alice.martin@example.com")
		@Email @Size(max = EMAIL_MAX_LENGTH) String email,
		@Schema(example = "Alice")
		@Size(max = PERSON_NAME_MAX_LENGTH) String firstName,
		@Schema(example = "Martin")
		@Size(max = PERSON_NAME_MAX_LENGTH) String lastName) {

	@AssertTrue(message = "At least one non-blank profile field must be provided")
	public boolean isValidPatch() {
		return (email != null || firstName != null || lastName != null)
				&& isNullOrHasValue(email)
				&& isNullOrHasValue(firstName)
				&& isNullOrHasValue(lastName);
	}

	private static boolean isNullOrHasValue(String value) {
		return value == null || !value.isBlank();
	}
}
