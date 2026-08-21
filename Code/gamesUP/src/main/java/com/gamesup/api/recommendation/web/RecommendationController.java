package com.gamesup.api.recommendation.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.config.web.OpenApiConfiguration;
import com.gamesup.api.recommendation.application.RecommendationService;
import com.gamesup.api.recommendation.web.dto.RecommendationResponse;

@Validated
@RestController
@PreAuthorize("hasRole('CLIENT')")
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations", description = "Jeux recommandés au CLIENT connecté.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class RecommendationController {

	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping
	@Operation(summary = "Obtenir ses recommandations")
	public RecommendationResponse recommend(
			@AuthenticationPrincipal GamesUpUserPrincipal principal,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return recommendationService.recommend(principal.userId(), limit);
	}
}
