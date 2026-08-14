package com.gamesup.api.order.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.gamesup.api.order.domain.OrderStatus;

public record OrderResponse(
		Long id,
		Long customerId,
		OrderStatus status,
		BigDecimal total,
		Instant createdAt,
		Instant updatedAt,
		List<OrderLineResponse> lines) {

	public OrderResponse {
		lines = List.copyOf(lines);
	}
}
