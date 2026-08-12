package com.gamesup.api.catalog.application;

import static com.gamesup.api.catalog.infrastructure.persistence.GameSpecifications.containsText;
import static com.gamesup.api.catalog.infrastructure.persistence.GameSpecifications.hasCategory;
import static com.gamesup.api.catalog.infrastructure.persistence.GameSpecifications.hasPriceBetween;
import static com.gamesup.api.catalog.infrastructure.persistence.GameSpecifications.isActive;
import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.web.dto.GameDetailResponse;
import com.gamesup.api.catalog.web.dto.GameSummaryResponse;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.common.web.dto.PageResponse;

@Service
public class GameQueryService {

	private static final String DEFAULT_SORT = "name,asc";
	private static final Map<String, String> SORT_PROPERTIES = Map.of(
			"id", "id",
			"name", "name",
			"price", "price",
			"minAge", "minAge",
			"durationMinutes", "durationMinutes",
			"editionNumber", "editionNumber");

	private final GameRepository gameRepository;
	private final GameResponseMapper gameResponseMapper;

	public GameQueryService(GameRepository gameRepository, GameResponseMapper gameResponseMapper) {
		this.gameRepository = gameRepository;
		this.gameResponseMapper = gameResponseMapper;
	}

	@Transactional(readOnly = true)
	public PageResponse<GameSummaryResponse> search(GameSearchCriteria criteria) {
		Objects.requireNonNull(criteria, "criteria must not be null");
		validateCriteria(criteria);

		Specification<Game> specification = isActive(true)
				.and(containsText(criteria.query()))
				.and(hasCategory(criteria.category()))
				.and(hasPriceBetween(criteria.minimumPrice(), criteria.maximumPrice()));
		PageRequest pageRequest = PageRequest.of(
				criteria.page(),
				criteria.size(),
				parseSort(criteria.sort()));
		Page<Game> games = gameRepository.findAll(specification, pageRequest);
		return PageResponse.from(games, gameResponseMapper::toSummary);
	}

	@Transactional(readOnly = true)
	public GameDetailResponse findActiveById(Long id) {
		if (id == null || id < 1) {
			throw new InvalidRequestException("Game identifier must be greater than zero.");
		}
		Game game = gameRepository.findByIdAndActiveTrue(id)
				.orElseThrow(() -> new ResourceNotFoundException("Game " + id + " was not found."));
		return gameResponseMapper.toResponse(game);
	}

	private static void validateCriteria(GameSearchCriteria criteria) {
		if (criteria.page() < 0) {
			throw new InvalidRequestException("Page must be greater than or equal to zero.");
		}
		if (criteria.size() < 1 || criteria.size() > PAGE_SIZE_MAX) {
			throw new InvalidRequestException("Page size must be between 1 and " + PAGE_SIZE_MAX + ".");
		}
		validatePrice(criteria.minimumPrice(), "Minimum price");
		validatePrice(criteria.maximumPrice(), "Maximum price");
		if (criteria.minimumPrice() != null
				&& criteria.maximumPrice() != null
				&& criteria.minimumPrice().compareTo(criteria.maximumPrice()) > 0) {
			throw new InvalidRequestException("Minimum price must not exceed maximum price.");
		}
	}

	private static void validatePrice(BigDecimal price, String label) {
		if (price != null && price.signum() < 0) {
			throw new InvalidRequestException(label + " must be greater than or equal to zero.");
		}
	}

	private static Sort parseSort(String value) {
		String sortValue = value == null || value.isBlank() ? DEFAULT_SORT : value.trim();
		String[] parts = sortValue.split(",", -1);
		if (parts.length > 2 || parts[0].isBlank()) {
			throw invalidSort();
		}

		String requestedProperty = parts[0].trim();
		String property = SORT_PROPERTIES.get(requestedProperty);
		if (property == null) {
			throw invalidSort();
		}

		String requestedDirection = parts.length == 2 ? parts[1].trim() : "asc";
		Sort.Direction direction = Sort.Direction.fromOptionalString(requestedDirection)
				.orElseThrow(GameQueryService::invalidSort);
		Sort sort = Sort.by(new Sort.Order(direction, property));
		return "id".equals(property) ? sort : sort.and(Sort.by("id"));
	}

	private static InvalidRequestException invalidSort() {
		return new InvalidRequestException(
				"Sort must use an allowed field and direction: "
						+ String.join(", ", SORT_PROPERTIES.keySet())
						+ "; asc or desc.");
	}
}
