package com.gamesup.api.common.web.dto;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public PageResponse {
		content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
		if (page < 0) {
			throw new IllegalArgumentException("page must be greater than or equal to zero");
		}
		if (size < 0) {
			throw new IllegalArgumentException("size must be greater than or equal to zero");
		}
		if (totalElements < 0) {
			throw new IllegalArgumentException("totalElements must be greater than or equal to zero");
		}
		if (totalPages < 0) {
			throw new IllegalArgumentException("totalPages must be greater than or equal to zero");
		}
	}

	public static <T> PageResponse<T> from(Page<T> source) {
		Objects.requireNonNull(source, "source must not be null");
		return new PageResponse<>(
				source.getContent(),
				source.getNumber(),
				source.getSize(),
				source.getTotalElements(),
				source.getTotalPages());
	}

	public static <S, T> PageResponse<T> from(
			Page<S> source,
			Function<? super S, ? extends T> mapper) {
		Objects.requireNonNull(source, "source must not be null");
		Objects.requireNonNull(mapper, "mapper must not be null");
		return from(source.map(mapper));
	}
}
