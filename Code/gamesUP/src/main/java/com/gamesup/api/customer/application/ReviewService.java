package com.gamesup.api.customer.application;

import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.common.application.exception.ConflictException;
import com.gamesup.api.common.application.exception.ForbiddenOperationException;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.common.web.dto.PageResponse;
import com.gamesup.api.customer.domain.Review;
import com.gamesup.api.customer.infrastructure.persistence.ReviewRepository;
import com.gamesup.api.customer.web.dto.ReviewResponse;

@Service
public class ReviewService {

	private static final int COMMENT_MAX_LENGTH = 2000;

	private final UserRepository userRepository;
	private final GameRepository gameRepository;
	private final ReviewRepository reviewRepository;
	private final ReviewMapper reviewMapper;

	public ReviewService(
			UserRepository userRepository,
			GameRepository gameRepository,
			ReviewRepository reviewRepository,
			ReviewMapper reviewMapper) {
		this.userRepository = userRepository;
		this.gameRepository = gameRepository;
		this.reviewRepository = reviewRepository;
		this.reviewMapper = reviewMapper;
	}

	@Transactional(readOnly = true)
	public PageResponse<ReviewResponse> findPublishedByGame(Long gameId, int page, int size) {
		requireActiveGame(gameId);
		validatePage(page, size);
		PageRequest pageRequest = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
		Page<Review> reviews = reviewRepository.findAllByGameIdAndHiddenFalse(gameId, pageRequest);
		return PageResponse.from(reviews, reviewMapper::toResponse);
	}

	@Transactional
	public ReviewResponse create(Long userId, Long gameId, int rating, String comment) {
		validateReview(rating, comment);
		User user = findUser(userId);
		Game game = requireActiveGame(gameId);
		if (reviewRepository.existsByUserIdAndGameId(userId, gameId)) {
			throw duplicateReview();
		}
		Review review = new Review(user, game, rating, normalizeComment(comment));
		try {
			return reviewMapper.toResponse(reviewRepository.saveAndFlush(review));
		} catch (DataIntegrityViolationException exception) {
			throw duplicateReview();
		}
	}

	@Transactional
	public ReviewResponse update(Long userId, Long reviewId, int rating, String comment) {
		validateReview(rating, comment);
		Review review = findOwnedReview(userId, reviewId);
		review.update(rating, normalizeComment(comment));
		reviewRepository.flush();
		return reviewMapper.toResponse(review);
	}

	@Transactional
	public void delete(Long userId, Long reviewId) {
		reviewRepository.delete(findOwnedReview(userId, reviewId));
	}

	@Transactional
	public void changeVisibility(Long reviewId, boolean hidden) {
		Review review = findReview(reviewId);
		review.setHidden(hidden);
	}

	private Review findOwnedReview(Long userId, Long reviewId) {
		Review review = findReview(reviewId);
		if (!review.getUser().getId().equals(userId)) {
			throw new ForbiddenOperationException("Only the review owner may modify or delete it.");
		}
		return review;
	}

	private Review findReview(Long reviewId) {
		return reviewRepository.findById(reviewId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Review " + reviewId + " was not found."));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User " + userId + " was not found."));
	}

	private Game requireActiveGame(Long gameId) {
		return gameRepository.findByIdAndActiveTrue(gameId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Active game " + gameId + " was not found."));
	}

	private static void validatePage(int page, int size) {
		if (page < 0) {
			throw new InvalidRequestException("Page must be greater than or equal to zero.");
		}
		if (size < 1 || size > PAGE_SIZE_MAX) {
			throw new InvalidRequestException("Page size must be between 1 and " + PAGE_SIZE_MAX + ".");
		}
	}

	private static void validateReview(int rating, String comment) {
		if (rating < 1 || rating > 5) {
			throw new InvalidRequestException("Rating must be an integer between 1 and 5.");
		}
		if (comment != null && comment.length() > COMMENT_MAX_LENGTH) {
			throw new InvalidRequestException(
					"Comment must not exceed " + COMMENT_MAX_LENGTH + " characters.");
		}
	}

	private static String normalizeComment(String comment) {
		if (comment == null || comment.isBlank()) {
			return null;
		}
		return comment.trim();
	}

	private static ConflictException duplicateReview() {
		return new ConflictException("A review already exists for this user and game.");
	}
}
