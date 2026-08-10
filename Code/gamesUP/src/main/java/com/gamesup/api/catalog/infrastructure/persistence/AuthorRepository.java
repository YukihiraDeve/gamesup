package com.gamesup.api.catalog.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.catalog.domain.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {

	Optional<Author> findByNormalizedName(String normalizedName);

	boolean existsByNormalizedName(String normalizedName);
}
