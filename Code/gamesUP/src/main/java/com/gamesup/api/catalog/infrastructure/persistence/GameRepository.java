package com.gamesup.api.catalog.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.gamesup.api.catalog.domain.Game;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

	Optional<Game> findByIdAndActiveTrue(Long id);

	boolean existsByPublisherId(Long publisherId);

	boolean existsByAuthorsId(Long authorId);

	boolean existsByCategoriesId(Long categoryId);
}
