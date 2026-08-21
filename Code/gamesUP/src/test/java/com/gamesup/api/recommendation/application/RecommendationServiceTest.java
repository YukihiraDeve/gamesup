package com.gamesup.api.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.gamesup.api.customer.infrastructure.persistence.ReviewRepository;
import com.gamesup.api.customer.infrastructure.persistence.ReviewRepository.RecommendationReviewView;
import com.gamesup.api.order.domain.OrderStatus;
import com.gamesup.api.order.infrastructure.persistence.OrderLineRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderLineRepository.RecommendationPurchaseView;
import com.gamesup.api.recommendation.application.port.RecommendationGateway;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.Candidate;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.Interaction;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.RecommendationResult;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.TrainingResult;
import com.gamesup.api.recommendation.web.dto.RecommendedGameResponse;

class RecommendationServiceTest {

	@Test
	void exportsPurchasesAtThreeAndLetsExplicitReviewsTakePriority() {
		RecommendationGateway gateway = mock(RecommendationGateway.class);
		OrderLineRepository orders = mock(OrderLineRepository.class);
		ReviewRepository reviews = mock(ReviewRepository.class);
		RecommendationGameEnricher enricher = mock(RecommendationGameEnricher.class);
		RecommendationService service = new RecommendationService(gateway, orders, reviews, enricher);
		when(orders.findRecommendationPurchases(OrderStatus.CANCELLED)).thenReturn(List.of(
				new PurchaseView(2L, 8L),
				new PurchaseView(1L, 4L),
				new PurchaseView(1L, 4L)));
		when(reviews.findRecommendationReviews()).thenReturn(List.of(
				new ReviewView(1L, 4L, 5),
				new ReviewView(1L, 6L, 2)));
		when(gateway.train(anyList())).thenReturn(new TrainingResult("knn", 2, 3, 3));

		var response = service.train();

		ArgumentCaptor<List<Interaction>> interactions = ArgumentCaptor.forClass(List.class);
		verify(gateway).train(interactions.capture());
		assertThat(interactions.getValue()).containsExactly(
				new Interaction(1L, 4L, 5.0),
				new Interaction(1L, 6L, 2.0),
				new Interaction(2L, 8L, 3.0));
		assertThat(response.retainedInteractions()).isEqualTo(3);
	}

	@Test
	void buildsOnlyTheCurrentUsersHistoryAndDelegatesOrderedEnrichment() {
		RecommendationGateway gateway = mock(RecommendationGateway.class);
		OrderLineRepository orders = mock(OrderLineRepository.class);
		ReviewRepository reviews = mock(ReviewRepository.class);
		RecommendationGameEnricher enricher = mock(RecommendationGameEnricher.class);
		RecommendationService service = new RecommendationService(gateway, orders, reviews, enricher);
		when(orders.findRecommendationPurchasesByUserId(7L, OrderStatus.CANCELLED))
				.thenReturn(List.of(new PurchaseView(7L, 4L)));
		when(reviews.findRecommendationReviewsByUserId(7L))
				.thenReturn(List.of(new ReviewView(7L, 4L, 4)));
		List<Candidate> candidates = List.of(new Candidate(9L, 0.8), new Candidate(5L, 0.7));
		when(gateway.recommend(7L, List.of(new Interaction(7L, 4L, 4.0)), 2))
				.thenReturn(new RecommendationResult("knn", candidates));
		when(enricher.enrich(candidates)).thenReturn(List.<RecommendedGameResponse>of());

		var response = service.recommend(7L, 2);

		assertThat(response.version()).isEqualTo("knn");
		verify(enricher).enrich(candidates);
	}

	private record PurchaseView(Long userId, Long gameId) implements RecommendationPurchaseView {

		@Override
		public Long getUserId() {
			return userId;
		}

		@Override
		public Long getGameId() {
			return gameId;
		}
	}

	private record ReviewView(Long userId, Long gameId, int rating) implements RecommendationReviewView {

		@Override
		public Long getUserId() {
			return userId;
		}

		@Override
		public Long getGameId() {
			return gameId;
		}

		@Override
		public int getRating() {
			return rating;
		}
	}
}
