package com.gamesup.api.catalog.infrastructure.persistence;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.gamesup.api.catalog.domain.Author;
import com.gamesup.api.catalog.domain.CatalogNameNormalizer;
import com.gamesup.api.catalog.domain.Category;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public final class GameSpecifications {

	private GameSpecifications() {
	}

	public static Specification<Game> isActive(boolean active) {
		return (root, query, builder) -> builder.equal(root.get("active"), active);
	}

	public static Specification<Game> containsText(String text) {
		return (root, query, builder) -> {
			String normalizedText = CatalogNameNormalizer.normalizedName(text);
			if (normalizedText == null || normalizedText.isBlank()) {
				return builder.conjunction();
			}

			Join<Game, Author> authors = root.join("authors", JoinType.LEFT);
			Join<Game, Publisher> publisher = root.join("publisher", JoinType.LEFT);
			query.distinct(true);
			String pattern = "%" + normalizedText + "%";

			return builder.or(
					builder.like(builder.lower(root.<String>get("name")), pattern),
					builder.like(authors.<String>get("normalizedName"), pattern),
					builder.like(publisher.<String>get("normalizedName"), pattern));
		};
	}

	public static Specification<Game> hasCategory(String categoryName) {
		return (root, query, builder) -> {
			String normalizedName = CatalogNameNormalizer.normalizedName(categoryName);
			if (normalizedName == null || normalizedName.isBlank()) {
				return builder.conjunction();
			}

			Join<Game, Category> categories = root.join("categories", JoinType.INNER);
			query.distinct(true);
			return builder.equal(categories.<String>get("normalizedName"), normalizedName);
		};
	}

	public static Specification<Game> hasPriceBetween(BigDecimal minimum, BigDecimal maximum) {
		return (root, query, builder) -> {
			if (minimum != null && maximum != null) {
				return builder.between(root.<BigDecimal>get("price"), minimum, maximum);
			}
			if (minimum != null) {
				return builder.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minimum);
			}
			if (maximum != null) {
				return builder.lessThanOrEqualTo(root.<BigDecimal>get("price"), maximum);
			}
			return builder.conjunction();
		};
	}
}
