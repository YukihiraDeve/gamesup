package com.gamesup.api.recommendation.web.dto;

import com.gamesup.api.catalog.web.dto.GameSummaryResponse;

public record RecommendedGameResponse(double score, GameSummaryResponse game) {
}
