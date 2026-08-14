package com.gamesup.api.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.web.dto.GameDetailResponse;
import com.gamesup.api.catalog.web.dto.GameSummaryResponse;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GameQueryServiceUnitTest {

	@Mock
	private GameRepository gameRepository;

	@Mock
	private GameResponseMapper gameResponseMapper;

	@Mock
	private Game game;

	@Mock
	private GameSummaryResponse summary;

	@Mock
	private GameDetailResponse detail;

	private GameQueryService gameQueryService;

	@BeforeEach
	void setUp() {
		gameQueryService = new GameQueryService(gameRepository, gameResponseMapper);
	}

	@Test
	void buildsDeterministicPaginationAndMapsResults() {
		when(gameRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(game)));
		when(gameResponseMapper.toSummary(game)).thenReturn(summary);

		var response = gameQueryService.search(criteria(0, 20, "price,desc", null, null));

		assertThat(response.content()).containsExactly(summary);
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(gameRepository).findAll(any(Specification.class), pageable.capture());
		assertThat(pageable.getValue().getSort().getOrderFor("price").isDescending()).isTrue();
		assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
	}

	@Test
	void appliesDefaultSortWhenSortIsBlank() {
		when(gameRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));

		gameQueryService.search(criteria(0, 20, "  ", null, null));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(gameRepository).findAll(any(Specification.class), pageable.capture());
		assertThat(pageable.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
	}

	@Test
	void rejectsInvalidPaginationPricesAndSorts() {
		assertThatThrownBy(() -> gameQueryService.search(null))
				.isInstanceOf(NullPointerException.class);
		assertInvalid(criteria(-1, 20, "name,asc", null, null));
		assertInvalid(criteria(0, 0, "name,asc", null, null));
		assertInvalid(criteria(0, 101, "name,asc", null, null));
		assertInvalid(criteria(0, 20, "name,asc", new BigDecimal("-1"), null));
		assertInvalid(criteria(0, 20, "name,asc", null, new BigDecimal("-1")));
		assertInvalid(criteria(0, 20, "name,asc", new BigDecimal("20"), new BigDecimal("10")));
		assertInvalid(criteria(0, 20, "unknown,asc", null, null));
		assertInvalid(criteria(0, 20, "name,sideways", null, null));
		assertInvalid(criteria(0, 20, "name,asc,extra", null, null));
		assertInvalid(criteria(0, 20, ",asc", null, null));
	}

	@Test
	void validatesIdentifiersAndMapsOnlyActiveGames() {
		assertThatThrownBy(() -> gameQueryService.findActiveById(null))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> gameQueryService.findActiveById(0L))
				.isInstanceOf(InvalidRequestException.class);

		when(gameRepository.findByIdAndActiveTrue(42L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> gameQueryService.findActiveById(42L))
				.isInstanceOf(ResourceNotFoundException.class);

		when(gameRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(game));
		when(gameResponseMapper.toResponse(game)).thenReturn(detail);
		assertThat(gameQueryService.findActiveById(7L)).isSameAs(detail);
	}

	private void assertInvalid(GameSearchCriteria criteria) {
		assertThatThrownBy(() -> gameQueryService.search(criteria))
				.isInstanceOf(InvalidRequestException.class);
	}

	private static GameSearchCriteria criteria(
			int page,
			int size,
			String sort,
			BigDecimal minimumPrice,
			BigDecimal maximumPrice) {
		return new GameSearchCriteria(
				null,
				null,
				minimumPrice,
				maximumPrice,
				page,
				size,
				sort);
	}
}
