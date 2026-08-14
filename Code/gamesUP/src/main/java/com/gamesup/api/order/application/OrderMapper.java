package com.gamesup.api.order.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.gamesup.api.order.domain.Order;
import com.gamesup.api.order.domain.OrderLine;
import com.gamesup.api.order.web.dto.OrderLineResponse;
import com.gamesup.api.order.web.dto.OrderResponse;

@Component
public class OrderMapper {

	public OrderResponse toResponse(Order order, List<OrderLine> lines) {
		List<OrderLineResponse> lineResponses = lines.stream()
				.map(this::toLineResponse)
				.toList();
		BigDecimal total = lineResponses.stream()
				.map(OrderLineResponse::lineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new OrderResponse(
				order.getId(),
				order.getUser().getId(),
				order.getStatus(),
				total,
				order.getCreatedAt(),
				order.getUpdatedAt(),
				lineResponses);
	}

	private OrderLineResponse toLineResponse(OrderLine line) {
		BigDecimal lineTotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
		return new OrderLineResponse(
				line.getGame().getId(),
				line.getGame().getName(),
				line.getQuantity(),
				line.getUnitPrice(),
				lineTotal);
	}
}
