package com.gamesup.api.customer.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.customer.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	Optional<Review> findByUserIdAndGameId(Long userId, Long gameId);

	boolean existsByUserIdAndGameId(Long userId, Long gameId);

	@EntityGraph(attributePaths = "user")
	Page<Review> findAllByGameIdAndHiddenFalse(Long gameId, Pageable pageable);
}
