package com.gamesup.api.order.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(
		@NotNull @Positive Long gameId,
		@NotNull @Min(1) Integer quantity) {
}
