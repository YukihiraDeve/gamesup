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

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminGameCreateRequest(
		@Schema(example = "Explorateurs du Levant")
		@NotBlank @Size(max = RESOURCE_NAME_MAX_LENGTH) String name,
		@Schema(example = "Un jeu d'exploration et de stratégie pour toute la famille.")
		@NotBlank @Size(max = 4000) String description,
		@Schema(example = "39.90")
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
		@Schema(example = "2")
		@NotNull @Min(1) Integer minPlayers,
		@Schema(example = "5")
		@NotNull @Min(1) Integer maxPlayers,
		@Schema(example = "10")
		@NotNull @Min(0) Integer minAge,
		@Schema(example = "60")
		@NotNull @Min(1) Integer durationMinutes,
		@Schema(example = "1")
		@NotNull @Min(1) Integer editionNumber,
		@Schema(example = "3")
		@NotNull @Positive Long publisherId,
		@Schema(example = "[4, 7]")
		@NotEmpty Set<@NotNull @Positive Long> authorIds,
		@Schema(example = "[2, 6]")
		@NotEmpty Set<@NotNull @Positive Long> categoryIds) {

	@AssertTrue(message = "maxPlayers must be greater than or equal to minPlayers")
	public boolean isPlayerRangeValid() {
		return minPlayers == null || maxPlayers == null || maxPlayers >= minPlayers;
	}
}
