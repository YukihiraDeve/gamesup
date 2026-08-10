package com.gamesup.api.catalog.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.catalog.domain.Publisher;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

	Optional<Publisher> findByNormalizedName(String normalizedName);

	boolean existsByNormalizedName(String normalizedName);
}
