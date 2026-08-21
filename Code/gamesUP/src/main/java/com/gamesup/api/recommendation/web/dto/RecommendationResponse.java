package com.gamesup.api.recommendation.web.dto;

import java.util.List;

public record RecommendationResponse(String version, List<RecommendedGameResponse> items) {

	public RecommendationResponse {
		items = List.copyOf(items);
	}
}
