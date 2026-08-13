package com.gamesup.api.catalog.web.admin.dto;

import static com.gamesup.api.common.web.validation.ValidationRules.RESOURCE_NAME_MAX_LENGTH;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminGameCreateRequest(
		@NotBlank @Size(max = RESOURCE_NAME_MAX_LENGTH) String name,
		@NotBlank @Size(max = 4000) String description,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
		@NotNull @Min(1) Integer minPlayers,
		@NotNull @Min(1) Integer maxPlayers,
		@NotNull @Min(0) Integer minAge,
		@NotNull @Min(1) Integer durationMinutes,
		@NotNull @Min(1) Integer editionNumber,
		@NotNull @Positive Long publisherId,
		@NotEmpty Set<@NotNull @Positive Long> authorIds,
		@NotEmpty Set<@NotNull @Positive Long> categoryIds) {

	@AssertTrue(message = "maxPlayers must be greater than or equal to minPlayers")
	public boolean isPlayerRangeValid() {
		return minPlayers == null || maxPlayers == null || maxPlayers >= minPlayers;
	}
}
