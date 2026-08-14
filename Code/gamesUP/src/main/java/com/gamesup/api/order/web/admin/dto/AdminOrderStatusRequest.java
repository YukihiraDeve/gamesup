package com.gamesup.api.order.web.admin.dto;

import jakarta.validation.constraints.NotNull;

import com.gamesup.api.order.domain.OrderStatus;

public record AdminOrderStatusRequest(@NotNull OrderStatus status) {
}
