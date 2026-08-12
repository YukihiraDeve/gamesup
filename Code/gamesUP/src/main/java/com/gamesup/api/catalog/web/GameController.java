package com.gamesup.api.catalog.web;

import static com.gamesup.api.common.web.validation.ValidationRules.CATALOG_NAME_MAX_LENGTH;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_MIN;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MIN;
import static com.gamesup.api.common.web.validation.ValidationRules.RESOURCE_NAME_MAX_LENGTH;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gamesup.api.catalog.application.GameQueryService;
import com.gamesup.api.catalog.application.GameSearchCriteria;
import com.gamesup.api.catalog.web.dto.GameDetailResponse;
import com.gamesup.api.catalog.web.dto.GameSummaryResponse;
import com.gamesup.api.common.web.dto.PageResponse;

@Validated
@RestController
@RequestMapping("/api/v1/games")
public class GameController {

	private final GameQueryService gameQueryService;

	public GameController(GameQueryService gameQueryService) {
		this.gameQueryService = gameQueryService;
	}

	@GetMapping
	public PageResponse<GameSummaryResponse> search(
			@RequestParam(name = "q", required = false) @Size(max = RESOURCE_NAME_MAX_LENGTH) String query,
			@RequestParam(required = false) @Size(max = CATALOG_NAME_MAX_LENGTH) String category,
			@RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
			@RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
			@RequestParam(defaultValue = "0") @Min(PAGE_MIN) int page,
			@RequestParam(defaultValue = "20") @Min(PAGE_SIZE_MIN) @Max(PAGE_SIZE_MAX) int size,
			@RequestParam(defaultValue = "name,asc") @Size(max = 50) String sort) {
		return gameQueryService.search(new GameSearchCriteria(
				query,
				category,
				minPrice,
				maxPrice,
				page,
				size,
				sort));
	}

	@GetMapping("/{id}")
	public GameDetailResponse findById(@PathVariable @Positive Long id) {
		return gameQueryService.findActiveById(id);
	}
}
