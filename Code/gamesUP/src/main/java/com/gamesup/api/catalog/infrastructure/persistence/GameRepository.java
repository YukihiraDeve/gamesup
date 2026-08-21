package com.gamesup.api.catalog.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.gamesup.api.catalog.domain.Game;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

	Optional<Game> findByIdAndActiveTrue(Long id);

	@EntityGraph(attributePaths = {"publisher", "authors", "categories"})
	List<Game> findAllByIdInAndActiveTrue(Collection<Long> ids);

	boolean existsByPublisherId(Long publisherId);

	boolean existsByAuthorsId(Long authorId);

	boolean existsByCategoriesId(Long categoryId);
}
