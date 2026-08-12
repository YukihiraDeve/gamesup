package com.gamesup.api.order.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.order.domain.OrderLine;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

	List<OrderLine> findAllByOrderId(Long orderId);
}
