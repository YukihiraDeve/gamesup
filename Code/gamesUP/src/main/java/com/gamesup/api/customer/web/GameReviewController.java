package com.gamesup.api.customer.web;

import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_MIN;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MIN;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.common.web.dto.PageResponse;
import com.gamesup.api.customer.application.ReviewService;
import com.gamesup.api.customer.web.dto.ReviewRequest;
import com.gamesup.api.customer.web.dto.ReviewResponse;

@Validated
@RestController
@RequestMapping("/api/v1/games/{gameId}/reviews")
public class GameReviewController {

	private final ReviewService reviewService;

	public GameReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping
	public PageResponse<ReviewResponse> findPublishedByGame(
			@PathVariable @Positive Long gameId,
			@RequestParam(defaultValue = "0") @Min(PAGE_MIN) int page,
			@RequestParam(defaultValue = "20") @Min(PAGE_SIZE_MIN) @Max(PAGE_SIZE_MAX) int size) {
		return reviewService.findPublishedByGame(gameId, page, size);
	}

	@PostMapping
	@PreAuthorize("hasRole('CLIENT')")
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewResponse create(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long gameId,
			@Valid @RequestBody ReviewRequest request) {
		return reviewService.create(
				principal.userId(), gameId, request.integerRating(), request.comment());
	}
}
