package com.gamesup.api.order.application;

import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.common.application.exception.BusinessRuleViolationException;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.common.web.dto.PageResponse;
import com.gamesup.api.order.domain.Order;
import com.gamesup.api.order.domain.OrderLine;
import com.gamesup.api.order.domain.OrderStatus;
import com.gamesup.api.order.domain.OrderStatusMachine;
import com.gamesup.api.order.infrastructure.persistence.OrderLineRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderRepository;
import com.gamesup.api.order.web.dto.OrderResponse;

@Service
public class AdminOrderService {

	private final OrderRepository orderRepository;
	private final OrderLineRepository orderLineRepository;
	private final OrderMapper orderMapper;

	public AdminOrderService(
			OrderRepository orderRepository,
			OrderLineRepository orderLineRepository,
			OrderMapper orderMapper) {
		this.orderRepository = orderRepository;
		this.orderLineRepository = orderLineRepository;
		this.orderMapper = orderMapper;
	}

	@Transactional(readOnly = true)
	public PageResponse<OrderResponse> findAll(int page, int size) {
		validatePage(page, size);
		Page<Order> orders = orderRepository.findAll(
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		Map<Long, List<OrderLine>> linesByOrder = findLinesByOrder(orders.getContent());
		List<OrderResponse> content = orders.stream()
				.map(order -> orderMapper.toResponse(
						order, linesByOrder.getOrDefault(order.getId(), List.of())))
				.toList();
		return new PageResponse<>(
				content,
				orders.getNumber(),
				orders.getSize(),
				orders.getTotalElements(),
				orders.getTotalPages());
	}

	@Transactional
	public OrderResponse transition(Long orderId, OrderStatus targetStatus) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Order " + orderId + " was not found."));
		if (!OrderStatusMachine.canTransition(order.getStatus(), targetStatus)) {
			throw new BusinessRuleViolationException(
					"Order cannot transition from " + order.getStatus() + " to " + targetStatus + ".");
		}
		order.changeStatus(targetStatus);
		orderRepository.flush();
		return orderMapper.toResponse(order, orderLineRepository.findAllByOrderId(orderId));
	}

	private Map<Long, List<OrderLine>> findLinesByOrder(List<Order> orders) {
		if (orders.isEmpty()) {
			return Map.of();
		}
		List<Long> orderIds = orders.stream().map(Order::getId).toList();
		return orderLineRepository.findAllByOrderIdInOrderByOrderIdAscIdAsc(orderIds)
				.stream()
				.collect(Collectors.groupingBy(line -> line.getOrder().getId()));
	}

	private static void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > PAGE_SIZE_MAX) {
			throw new InvalidRequestException(
					"Page must be non-negative and size must be between 1 and " + PAGE_SIZE_MAX + ".");
		}
	}
}
