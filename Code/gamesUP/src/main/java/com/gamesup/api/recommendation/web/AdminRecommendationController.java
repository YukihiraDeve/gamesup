package com.gamesup.api.recommendation.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.config.web.OpenApiConfiguration;
import com.gamesup.api.recommendation.application.RecommendationService;
import com.gamesup.api.recommendation.web.dto.TrainingModelResponse;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/recommendations")
@Tag(name = "Recommendation administration", description = "Entraînement du modèle réservé aux ADMIN.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AdminRecommendationController {

	private final RecommendationService recommendationService;

	public AdminRecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@PostMapping("/train")
	@Operation(summary = "Entraîner le modèle de recommandation")
	public TrainingModelResponse train() {
		return recommendationService.train();
	}
}
