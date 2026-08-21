package com.gamesup.api.recommendation.application.port;

import java.util.List;

public interface RecommendationGateway {

	TrainingResult train(List<Interaction> interactions);

	RecommendationResult recommend(long userId, List<Interaction> history, int limit);

	record Interaction(long userId, long gameId, double rating) {
	}

	record TrainingResult(String version, int users, int games, int retainedInteractions) {
	}

	record Candidate(long gameId, double score) {
	}

	record RecommendationResult(String version, List<Candidate> items) {

		public RecommendationResult {
			items = List.copyOf(items);
		}
	}
}
