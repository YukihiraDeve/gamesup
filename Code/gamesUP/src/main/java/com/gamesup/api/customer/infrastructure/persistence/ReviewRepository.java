package com.gamesup.api.customer.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.customer.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	Optional<Review> findByUserIdAndGameId(Long userId, Long gameId);

	List<Review> findAllByGameId(Long gameId);
}
