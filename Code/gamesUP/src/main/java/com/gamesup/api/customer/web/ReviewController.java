package com.gamesup.api.customer.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import com.gamesup.api.config.web.OpenApiConfiguration;
import com.gamesup.api.customer.application.ReviewService;
import com.gamesup.api.customer.web.dto.ReviewRequest;
import com.gamesup.api.customer.web.dto.ReviewResponse;

@Validated
@RestController
@PreAuthorize("hasRole('CLIENT')")
@RequestMapping("/api/v1/reviews")
@Tag(name = "Owned reviews", description = "Modification et suppression réservées au propriétaire.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PutMapping("/{reviewId}")
	@Operation(summary = "Modifier son avis")
	public ReviewResponse update(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long reviewId,
			@Valid @RequestBody ReviewRequest request) {
		return reviewService.update(
				principal.userId(), reviewId, request.integerRating(), request.comment());
	}

	@DeleteMapping("/{reviewId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Supprimer son avis")
	public void delete(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long reviewId) {
		reviewService.delete(principal.userId(), reviewId);
	}
}
