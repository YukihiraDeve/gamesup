package com.gamesup.api.customer.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewRequest(
		@Schema(example = "5", description = "Note entière de 1 à 5.")
		@NotNull
		@DecimalMin("1")
		@DecimalMax("5")
		@Digits(integer = 1, fraction = 0)
		BigDecimal rating,
		@Schema(example = "Accessible, rapide et très rejouable.")
		@Size(max = 2000)
		String comment) {

	public int integerRating() {
		return rating.intValueExact();
	}
}
