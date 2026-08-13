package com.gamesup.api.order.web.admin.dto;

public record InventoryResponse(Long id, Long gameId, int quantity, long version) {
}
