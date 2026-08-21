package com.gamesup.api.recommendation.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.catalog.application.GameResponseMapper;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.Candidate;
import com.gamesup.api.recommendation.web.dto.RecommendedGameResponse;

@Component
public class RecommendationGameEnricher {

	private final GameRepository gameRepository;
	private final GameResponseMapper gameResponseMapper;

	public RecommendationGameEnricher(
			GameRepository gameRepository,
			GameResponseMapper gameResponseMapper) {
		this.gameRepository = gameRepository;
		this.gameResponseMapper = gameResponseMapper;
	}

	@Transactional(readOnly = true)
	public List<RecommendedGameResponse> enrich(List<Candidate> candidates) {
		if (candidates.isEmpty()) {
			return List.of();
		}
		List<Long> ids = new LinkedHashSet<>(candidates.stream().map(Candidate::gameId).toList())
				.stream()
				.toList();
		Map<Long, Game> activeGamesById = gameRepository.findAllByIdInAndActiveTrue(ids).stream()
				.collect(Collectors.toMap(Game::getId, Function.identity()));

		return candidates.stream()
				.filter(candidate -> activeGamesById.containsKey(candidate.gameId()))
				.map(candidate -> new RecommendedGameResponse(
						candidate.score(),
						gameResponseMapper.toSummary(activeGamesById.get(candidate.gameId()))))
				.toList();
	}
}
