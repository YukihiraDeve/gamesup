package com.gamesup.api.catalog.application;

import java.util.Comparator;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.NamedCatalogEntity;
import com.gamesup.api.catalog.web.dto.GameDetailResponse;
import com.gamesup.api.catalog.web.dto.GameSummaryResponse;
import com.gamesup.api.common.application.mapping.ResponseMapper;

@Component
public class GameResponseMapper implements ResponseMapper<Game, GameDetailResponse> {

	private static final Comparator<String> NAME_ORDER =
			String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

	public GameSummaryResponse toSummary(Game game) {
		return new GameSummaryResponse(
				game.getId(),
				game.getName(),
				game.getPrice(),
				game.getMinPlayers(),
				game.getMaxPlayers(),
				game.getMinAge(),
				game.getDurationMinutes(),
				game.getPublisher().getName(),
				names(game.getAuthors()),
				names(game.getCategories()));
	}

	@Override
	public GameDetailResponse toResponse(Game game) {
		return new GameDetailResponse(
				game.getId(),
				game.getName(),
				game.getDescription(),
				game.getPrice(),
				game.getMinPlayers(),
				game.getMaxPlayers(),
				game.getMinAge(),
				game.getDurationMinutes(),
				game.getEditionNumber(),
				game.getPublisher().getName(),
				names(game.getAuthors()),
				names(game.getCategories()));
	}

	private static List<String> names(Collection<? extends NamedCatalogEntity> entities) {
		return entities.stream()
				.map(NamedCatalogEntity::getName)
				.sorted(NAME_ORDER)
				.toList();
	}
}
