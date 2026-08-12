package com.gamesup.api.catalog.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record GameDetailResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		int minPlayers,
		int maxPlayers,
		int minAge,
		int durationMinutes,
		int editionNumber,
		String publisher,
		List<String> authors,
		List<String> categories) {
}
