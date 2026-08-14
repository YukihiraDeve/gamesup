package com.gamesup.api.customer.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
import com.gamesup.api.config.web.OpenApiConfiguration;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/reviews")
@Tag(name = "Review administration", description = "Modération sans suppression physique, réservée aux ADMIN.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AdminReviewController {

	private final ReviewService reviewService;

	public AdminReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PatchMapping("/{reviewId}/visibility")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(
			summary = "Masquer ou republier un avis",
			responses = {
					@ApiResponse(responseCode = "204", description = "Visibilité modifiée."),
					@ApiResponse(responseCode = "400", description = "Requête invalide.",
							content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
					@ApiResponse(responseCode = "401", description = "JWT absent ou invalide."),
					@ApiResponse(responseCode = "403", description = "Rôle ADMIN requis."),
					@ApiResponse(responseCode = "404", description = "Avis introuvable.")
			})
	public void changeVisibility(
			@PathVariable @Positive Long reviewId,
			@Valid @RequestBody AdminReviewVisibilityRequest request) {
		reviewService.changeVisibility(reviewId, request.hidden());
	}
}
