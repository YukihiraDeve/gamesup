package com.gamesup.api.catalog.web.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminGameResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		int minPlayers,
		int maxPlayers,
		int minAge,
		int durationMinutes,
		int editionNumber,
		boolean active,
		CatalogReferenceResponse publisher,
		List<CatalogReferenceResponse> authors,
		List<CatalogReferenceResponse> categories) {
}
