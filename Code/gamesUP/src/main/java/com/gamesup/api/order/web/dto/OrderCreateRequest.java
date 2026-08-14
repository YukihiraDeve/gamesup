package com.gamesup.api.order.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
		@NotEmpty
		@Size(max = 100)
		List<@Valid OrderLineRequest> lines) {
}
