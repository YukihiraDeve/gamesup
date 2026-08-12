package com.gamesup.api.catalog.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record GameSummaryResponse(
		Long id,
		String name,
		BigDecimal price,
		int minPlayers,
		int maxPlayers,
		int minAge,
		int durationMinutes,
		String publisher,
		List<String> authors,
		List<String> categories) {
}
