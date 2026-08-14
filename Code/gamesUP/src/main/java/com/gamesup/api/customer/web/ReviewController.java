package com.gamesup.api.customer.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.customer.application.ReviewService;
import com.gamesup.api.customer.web.dto.ReviewRequest;
import com.gamesup.api.customer.web.dto.ReviewResponse;

@Validated
@RestController
@PreAuthorize("hasRole('CLIENT')")
@RequestMapping("/api/v1/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PutMapping("/{reviewId}")
	public ReviewResponse update(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long reviewId,
			@Valid @RequestBody ReviewRequest request) {
		return reviewService.update(
				principal.userId(), reviewId, request.integerRating(), request.comment());
	}

	@DeleteMapping("/{reviewId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long reviewId) {
		reviewService.delete(principal.userId(), reviewId);
	}
}
