package com.gamesup.api.recommendation.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gamesup.api.customer.infrastructure.persistence.ReviewRepository;
import com.gamesup.api.customer.infrastructure.persistence.ReviewRepository.RecommendationReviewView;
import com.gamesup.api.order.domain.OrderStatus;
import com.gamesup.api.order.infrastructure.persistence.OrderLineRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderLineRepository.RecommendationPurchaseView;
import com.gamesup.api.recommendation.application.port.RecommendationGateway;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.Interaction;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.RecommendationResult;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.TrainingResult;
import com.gamesup.api.recommendation.web.dto.RecommendationResponse;
import com.gamesup.api.recommendation.web.dto.TrainingModelResponse;

@Service
public class RecommendationService {

	private static final double IMPLICIT_PURCHASE_RATING = 3.0;
	private static final Comparator<InteractionKey> INTERACTION_ORDER = Comparator
			.comparingLong(InteractionKey::userId)
			.thenComparingLong(InteractionKey::gameId);

	private final RecommendationGateway recommendationGateway;
	private final OrderLineRepository orderLineRepository;
	private final ReviewRepository reviewRepository;
	private final RecommendationGameEnricher gameEnricher;

	public RecommendationService(
			RecommendationGateway recommendationGateway,
			OrderLineRepository orderLineRepository,
			ReviewRepository reviewRepository,
			RecommendationGameEnricher gameEnricher) {
		this.recommendationGateway = recommendationGateway;
		this.orderLineRepository = orderLineRepository;
		this.reviewRepository = reviewRepository;
		this.gameEnricher = gameEnricher;
	}

	public TrainingModelResponse train() {
		List<Interaction> interactions = mergeInteractions(
				orderLineRepository.findRecommendationPurchases(OrderStatus.CANCELLED),
				reviewRepository.findRecommendationReviews());
		TrainingResult result = recommendationGateway.train(interactions);
		return new TrainingModelResponse(
				result.version(),
				result.users(),
				result.games(),
				result.retainedInteractions());
	}

	public RecommendationResponse recommend(long userId, int limit) {
		List<Interaction> history = mergeInteractions(
				orderLineRepository.findRecommendationPurchasesByUserId(userId, OrderStatus.CANCELLED),
				reviewRepository.findRecommendationReviewsByUserId(userId));
		RecommendationResult result = recommendationGateway.recommend(userId, history, limit);
		return new RecommendationResponse(result.version(), gameEnricher.enrich(result.items()));
	}

	private static List<Interaction> mergeInteractions(
			List<RecommendationPurchaseView> purchases,
			List<RecommendationReviewView> reviews) {
		Map<InteractionKey, Double> ratings = new LinkedHashMap<>();
		purchases.stream()
				.map(purchase -> new InteractionKey(purchase.getUserId(), purchase.getGameId()))
				.sorted(INTERACTION_ORDER)
				.forEach(key -> ratings.putIfAbsent(key, IMPLICIT_PURCHASE_RATING));
		reviews.stream()
				.sorted(Comparator
						.comparing(RecommendationReviewView::getUserId)
						.thenComparing(RecommendationReviewView::getGameId))
				.forEach(review -> ratings.put(
						new InteractionKey(review.getUserId(), review.getGameId()),
						(double) review.getRating()));

		List<Map.Entry<InteractionKey, Double>> orderedRatings = new ArrayList<>(ratings.entrySet());
		orderedRatings.sort(Map.Entry.comparingByKey(INTERACTION_ORDER));
		return orderedRatings.stream()
				.map(entry -> new Interaction(
						entry.getKey().userId(),
						entry.getKey().gameId(),
						entry.getValue()))
				.toList();
	}

	private record InteractionKey(long userId, long gameId) {
	}
}
