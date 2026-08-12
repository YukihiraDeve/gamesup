package com.gamesup.api.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gamesup.api.catalog.application.GameQueryService;
import com.gamesup.api.catalog.application.GameSearchCriteria;
import com.gamesup.api.catalog.web.dto.GameDetailResponse;
import com.gamesup.api.catalog.web.dto.GameSummaryResponse;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.common.web.ApiExceptionHandler;
import com.gamesup.api.common.web.dto.PageResponse;

class GameControllerTest {

	private GameQueryService gameQueryService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		gameQueryService = org.mockito.Mockito.mock(GameQueryService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new GameController(gameQueryService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void forwardsCombinedSearchAndReturnsPageWithoutJpaFields() throws Exception {
		GameSummaryResponse game = new GameSummaryResponse(
				7L,
				"Cyclades",
				new BigDecimal("49.90"),
				2,
				5,
				10,
				60,
				"Matagot",
				List.of("Bruno Cathala"),
				List.of("Strategy"));
		when(gameQueryService.search(any())).thenReturn(new PageResponse<>(List.of(game), 1, 5, 6, 2));

		mockMvc.perform(get("/api/v1/games")
					.param("q", "Cathala")
					.param("category", "Strategy")
					.param("minPrice", "40.00")
					.param("maxPrice", "60.00")
					.param("page", "1")
					.param("size", "5")
					.param("sort", "price,desc"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content[0].name").value("Cyclades"))
				.andExpect(jsonPath("$.content[0].publisher").value("Matagot"))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.totalElements").value(6))
				.andExpect(jsonPath("$.content[0].hibernateLazyInitializer").doesNotExist())
				.andExpect(jsonPath("$.content[0].active").doesNotExist());

		ArgumentCaptor<GameSearchCriteria> criteria = ArgumentCaptor.forClass(GameSearchCriteria.class);
		verify(gameQueryService).search(criteria.capture());
		assertThat(criteria.getValue()).isEqualTo(new GameSearchCriteria(
				"Cathala",
				"Strategy",
				new BigDecimal("40.00"),
				new BigDecimal("60.00"),
				1,
				5,
				"price,desc"));
	}

	@Test
	void returnsGameDetailWithoutJpaInternals() throws Exception {
		when(gameQueryService.findActiveById(7L)).thenReturn(new GameDetailResponse(
				7L,
				"Azul",
				"Decorate the palace walls.",
				new BigDecimal("39.90"),
				2,
				4,
				8,
				45,
				2,
				"Plan B Games",
				List.of("Michael Kiesling"),
				List.of("Abstract")));

		mockMvc.perform(get("/api/v1/games/7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Azul"))
				.andExpect(jsonPath("$.description").value("Decorate the palace walls."))
				.andExpect(jsonPath("$.authors[0]").value("Michael Kiesling"))
				.andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist())
				.andExpect(jsonPath("$.active").doesNotExist());
	}

	@Test
	void mapsInvalidSortToBadRequest() throws Exception {
		when(gameQueryService.search(any())).thenThrow(new InvalidRequestException("Invalid sort."));

		mockMvc.perform(get("/api/v1/games").param("sort", "publisher,sideways"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void returnsNotFoundForUnknownOrInactiveGame() throws Exception {
		when(gameQueryService.findActiveById(404L))
				.thenThrow(new ResourceNotFoundException("Game 404 was not found."));

		mockMvc.perform(get("/api/v1/games/404"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void rejectsPageSizeAboveMaximum() throws Exception {
		when(gameQueryService.search(any()))
				.thenThrow(new InvalidRequestException("Page size must be between 1 and 100."));

		mockMvc.perform(get("/api/v1/games").param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}
}
