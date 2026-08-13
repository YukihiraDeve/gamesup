package com.gamesup.api.catalog.application;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.NamedCatalogEntity;
import com.gamesup.api.catalog.web.admin.dto.AdminGameResponse;
import com.gamesup.api.catalog.web.admin.dto.CatalogReferenceResponse;

@Component
public class AdminCatalogMapper {

	private static final Comparator<CatalogReferenceResponse> REFERENCE_ORDER =
			Comparator.comparing(CatalogReferenceResponse::name, String.CASE_INSENSITIVE_ORDER)
					.thenComparing(CatalogReferenceResponse::id);

	public AdminGameResponse toResponse(Game game) {
		return new AdminGameResponse(
				game.getId(),
				game.getName(),
				game.getDescription(),
				game.getPrice(),
				game.getMinPlayers(),
				game.getMaxPlayers(),
				game.getMinAge(),
				game.getDurationMinutes(),
				game.getEditionNumber(),
				game.isActive(),
				toReference(game.getPublisher()),
				toReferences(game.getAuthors()),
				toReferences(game.getCategories()));
	}

	public CatalogReferenceResponse toReference(NamedCatalogEntity entity) {
		return new CatalogReferenceResponse(entity.getId(), entity.getName());
	}

	public List<CatalogReferenceResponse> toReferences(
			Collection<? extends NamedCatalogEntity> entities) {
		return entities.stream()
				.map(this::toReference)
				.sorted(REFERENCE_ORDER)
				.toList();
	}
}
