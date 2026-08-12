package com.gamesup.api.catalog.application;

import java.math.BigDecimal;

public record GameSearchCriteria(
		String query,
		String category,
		BigDecimal minimumPrice,
		BigDecimal maximumPrice,
		int page,
		int size,
		String sort) {
}
