package com.gamesup.api.recommendation.web.dto;

public record TrainingModelResponse(
		String version,
		int users,
		int games,
		int retainedInteractions) {
}
