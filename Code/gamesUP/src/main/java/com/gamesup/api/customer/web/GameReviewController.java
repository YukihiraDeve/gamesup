package com.gamesup.api.customer.web;

import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_MIN;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MIN;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import com.gamesup.api.config.web.OpenApiConfiguration;
import com.gamesup.api.customer.application.ReviewService;
import com.gamesup.api.customer.web.dto.ReviewRequest;
import com.gamesup.api.customer.web.dto.ReviewResponse;

@Validated
@RestController
@RequestMapping("/api/v1/games/{gameId}/reviews")
@Tag(name = "Game reviews", description = "Avis publics d'un jeu et création par un CLIENT.")
public class GameReviewController {

	private final ReviewService reviewService;

	public GameReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping
	@Operation(
			summary = "Lister les avis publiés",
			description = "Les avis masqués sont exclus et seul le prénom public du client est exposé.")
	public PageResponse<ReviewResponse> findPublishedByGame(
			@PathVariable @Positive Long gameId,
			@RequestParam(defaultValue = "0") @Min(PAGE_MIN) int page,
			@RequestParam(defaultValue = "20") @Min(PAGE_SIZE_MIN) @Max(PAGE_SIZE_MAX) int size) {
		return reviewService.findPublishedByGame(gameId, page, size);
	}

	@PostMapping
	@PreAuthorize("hasRole('CLIENT')")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
			summary = "Créer son avis",
			description = "Un seul avis est autorisé par couple client/jeu.",
			security = @SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH))
	public ReviewResponse create(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@PathVariable @Positive Long gameId,
			@Valid @RequestBody ReviewRequest request) {
		return reviewService.create(
				principal.userId(), gameId, request.integerRating(), request.comment());
	}
}
