package com.gamesup.api.customer.web.dto;

import java.util.List;

import com.gamesup.api.catalog.web.dto.GameSummaryResponse;

public record WishlistResponse(List<GameSummaryResponse> games) {

	public WishlistResponse {
		games = List.copyOf(games);
	}
}
