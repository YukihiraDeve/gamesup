package com.gamesup.api.recommendation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gamesup.api.catalog.application.GameResponseMapper;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.web.dto.GameSummaryResponse;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.Candidate;

class RecommendationGameEnricherTest {

	@Test
	void preservesFastApiOrderAndIgnoresUnknownOrInactiveGames() {
		GameRepository games = mock(GameRepository.class);
		GameResponseMapper mapper = mock(GameResponseMapper.class);
		RecommendationGameEnricher enricher = new RecommendationGameEnricher(games, mapper);
		Game first = game(7L);
		Game last = game(5L);
		when(games.findAllByIdInAndActiveTrue(List.of(7L, 999L, 5L))).thenReturn(List.of(last, first));
		when(mapper.toSummary(first)).thenReturn(summary(7L));
		when(mapper.toSummary(last)).thenReturn(summary(5L));

		var response = enricher.enrich(List.of(
				new Candidate(7L, 0.9),
				new Candidate(999L, 0.8),
				new Candidate(5L, 0.7)));

		assertThat(response).extracting(item -> item.game().id()).containsExactly(7L, 5L);
		assertThat(response).extracting(item -> item.score()).containsExactly(0.9, 0.7);
	}

	private static Game game(long id) {
		Game game = mock(Game.class);
		when(game.getId()).thenReturn(id);
		return game;
	}

	private static GameSummaryResponse summary(long id) {
		return new GameSummaryResponse(
				id,
				"Game " + id,
				new BigDecimal("29.90"),
				1,
				4,
				10,
				45,
				"Publisher",
				List.of(),
				List.of());
	}
}
