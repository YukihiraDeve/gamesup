package com.gamesup.api.order.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gamesup.api.order.domain.OrderLine;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

	@EntityGraph(attributePaths = "game")
	List<OrderLine> findAllByOrderId(Long orderId);

	@EntityGraph(attributePaths = "game")
	List<OrderLine> findAllByOrderIdInOrderByOrderIdAscIdAsc(List<Long> orderIds);
}
