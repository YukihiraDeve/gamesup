package com.gamesup.api.order.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderLineRequest(
		@Schema(example = "12", description = "Identifiant d'un jeu actif.")
		@NotNull @Positive Long gameId,
		@Schema(example = "2")
		@NotNull @Min(1) Integer quantity) {
}
