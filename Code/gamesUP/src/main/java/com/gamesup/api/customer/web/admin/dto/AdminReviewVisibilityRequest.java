package com.gamesup.api.customer.web.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminReviewVisibilityRequest(@NotNull Boolean hidden) {
}
