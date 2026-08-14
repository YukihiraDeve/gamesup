package com.gamesup.api.order.web.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
		Long gameId,
		String gameName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal) {
}
