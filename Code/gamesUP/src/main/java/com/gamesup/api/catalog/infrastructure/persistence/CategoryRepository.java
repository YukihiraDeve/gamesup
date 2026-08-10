package com.gamesup.api.catalog.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.catalog.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findByNormalizedName(String normalizedName);

	boolean existsByNormalizedName(String normalizedName);
}
