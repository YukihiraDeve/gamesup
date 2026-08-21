package com.gamesup.api.order.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gamesup.api.order.domain.OrderLine;
import com.gamesup.api.order.domain.OrderStatus;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

	@EntityGraph(attributePaths = "game")
	List<OrderLine> findAllByOrderId(Long orderId);

	@EntityGraph(attributePaths = "game")
	List<OrderLine> findAllByOrderIdInOrderByOrderIdAscIdAsc(List<Long> orderIds);

	@Query("""
			select line.order.user.id as userId, line.game.id as gameId
			from OrderLine line
			where line.order.status <> :cancelledStatus
			order by line.order.user.id, line.game.id, line.id
			""")
	List<RecommendationPurchaseView> findRecommendationPurchases(
			@Param("cancelledStatus") OrderStatus cancelledStatus);

	@Query("""
			select line.order.user.id as userId, line.game.id as gameId
			from OrderLine line
			where line.order.user.id = :userId
			  and line.order.status <> :cancelledStatus
			order by line.game.id, line.id
			""")
	List<RecommendationPurchaseView> findRecommendationPurchasesByUserId(
			@Param("userId") Long userId,
			@Param("cancelledStatus") OrderStatus cancelledStatus);

	interface RecommendationPurchaseView {

		Long getUserId();

		Long getGameId();
	}
}
