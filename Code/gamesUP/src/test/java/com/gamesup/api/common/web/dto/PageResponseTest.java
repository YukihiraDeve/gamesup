package com.gamesup.api.common.web.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

	@Test
	void copiesPageContentAndMetadata() {
		PageImpl<String> page = new PageImpl<>(
				List.of("Azul", "Catan"),
				PageRequest.of(2, 2),
				7);

		PageResponse<Integer> response = PageResponse.from(page, String::length);

		assertThat(response.content()).containsExactly(4, 5);
		assertThat(response.page()).isEqualTo(2);
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(7);
		assertThat(response.totalPages()).isEqualTo(4);
	}

	@Test
	void keepsContentImmutable() {
		List<String> source = new ArrayList<>(List.of("Azul"));
		PageResponse<String> response = new PageResponse<>(source, 0, 1, 1, 1);

		source.add("Catan");

		assertThat(response.content()).containsExactly("Azul");
		assertThatThrownBy(() -> response.content().add("Catan"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void rejectsInvalidMetadata() {
		assertThatThrownBy(() -> new PageResponse<>(List.of(), -1, 20, 0, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("page");
		assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, -1, 0, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("size");
		assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, 20, -1, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("totalElements");
	}
}
