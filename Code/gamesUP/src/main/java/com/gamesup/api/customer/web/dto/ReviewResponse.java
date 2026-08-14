package com.gamesup.api.customer.web.dto;

import java.time.Instant;

public record ReviewResponse(
		Long id,
		Long gameId,
		int rating,
		String comment,
		String reviewerName,
		Instant createdAt,
		Instant updatedAt) {
}
