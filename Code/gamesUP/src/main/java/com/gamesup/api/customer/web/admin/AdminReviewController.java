package com.gamesup.api.customer.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.customer.application.ReviewService;
import com.gamesup.api.customer.web.admin.dto.AdminReviewVisibilityRequest;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/reviews")
public class AdminReviewController {

	private final ReviewService reviewService;

	public AdminReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PatchMapping("/{reviewId}/visibility")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changeVisibility(
			@PathVariable @Positive Long reviewId,
			@Valid @RequestBody AdminReviewVisibilityRequest request) {
		reviewService.changeVisibility(reviewId, request.hidden());
	}
}
