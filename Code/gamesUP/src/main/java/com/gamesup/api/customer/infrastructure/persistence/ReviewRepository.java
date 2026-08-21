package com.gamesup.api.customer.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gamesup.api.customer.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	Optional<Review> findByUserIdAndGameId(Long userId, Long gameId);

	boolean existsByUserIdAndGameId(Long userId, Long gameId);

	@EntityGraph(attributePaths = "user")
	Page<Review> findAllByGameIdAndHiddenFalse(Long gameId, Pageable pageable);

	@Query("""
			select review.user.id as userId, review.game.id as gameId, review.rating as rating
			from Review review
			order by review.user.id, review.game.id
			""")
	List<RecommendationReviewView> findRecommendationReviews();

	@Query("""
			select review.user.id as userId, review.game.id as gameId, review.rating as rating
			from Review review
			where review.user.id = :userId
			order by review.game.id
			""")
	List<RecommendationReviewView> findRecommendationReviewsByUserId(@Param("userId") Long userId);

	interface RecommendationReviewView {

		Long getUserId();

		Long getGameId();

		int getRating();
	}
}
