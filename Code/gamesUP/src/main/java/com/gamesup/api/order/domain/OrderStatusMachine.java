package com.gamesup.api.order.domain;

import java.util.Map;
import java.util.Set;

public final class OrderStatusMachine {

	private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
			OrderStatus.PENDING, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
			OrderStatus.PAID, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
			OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
			OrderStatus.DELIVERED, Set.of(OrderStatus.ARCHIVED),
			OrderStatus.CANCELLED, Set.of(OrderStatus.ARCHIVED),
			OrderStatus.ARCHIVED, Set.of());

	private OrderStatusMachine() {
	}

	public static boolean canTransition(OrderStatus current, OrderStatus target) {
		return TRANSITIONS.getOrDefault(current, Set.of()).contains(target);
	}
}
