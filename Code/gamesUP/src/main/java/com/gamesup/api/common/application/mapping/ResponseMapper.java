package com.gamesup.api.common.application.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@FunctionalInterface
public interface ResponseMapper<SOURCE, RESPONSE> {

	RESPONSE toResponse(SOURCE source);

	default List<RESPONSE> toResponseList(Iterable<SOURCE> sources) {
		Objects.requireNonNull(sources, "sources must not be null");
		List<RESPONSE> responses = new ArrayList<>();
		for (SOURCE source : sources) {
			responses.add(toResponse(source));
		}
		return List.copyOf(responses);
	}
}
