package com.gamesup.api.order.application;

import static com.gamesup.api.common.web.validation.ValidationRules.PAGE_SIZE_MAX;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.common.application.exception.BusinessRuleViolationException;
import com.gamesup.api.common.application.exception.ForbiddenOperationException;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.common.web.dto.PageResponse;
import com.gamesup.api.order.domain.Inventory;
import com.gamesup.api.order.domain.Order;
import com.gamesup.api.order.domain.OrderLine;
import com.gamesup.api.order.domain.OrderStatus;
import com.gamesup.api.order.infrastructure.persistence.InventoryRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderLineRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderRepository;
import com.gamesup.api.order.web.dto.OrderLineRequest;
import com.gamesup.api.order.web.dto.OrderResponse;

@Service
public class OrderService {

	private final UserRepository userRepository;
	private final GameRepository gameRepository;
	private final InventoryRepository inventoryRepository;
	private final OrderRepository orderRepository;
	private final OrderLineRepository orderLineRepository;
	private final OrderMapper orderMapper;

	public OrderService(
			UserRepository userRepository,
			GameRepository gameRepository,
			InventoryRepository inventoryRepository,
			OrderRepository orderRepository,
			OrderLineRepository orderLineRepository,
			OrderMapper orderMapper) {
		this.userRepository = userRepository;
		this.gameRepository = gameRepository;
		this.inventoryRepository = inventoryRepository;
		this.orderRepository = orderRepository;
		this.orderLineRepository = orderLineRepository;
		this.orderMapper = orderMapper;
	}

	@Transactional
	public OrderResponse create(Long userId, List<OrderLineRequest> requestedLines) {
		List<OrderLineRequest> lines = validateAndSortLines(requestedLines);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User " + userId + " was not found."));

		List<ReservedLine> reservedLines = new ArrayList<>(lines.size());
		for (OrderLineRequest line : lines) {
			Game game = gameRepository.findByIdAndActiveTrue(line.gameId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Active game " + line.gameId() + " was not found."));
			Inventory inventory = inventoryRepository.findByGameIdForUpdate(line.gameId())
					.orElseThrow(() -> insufficientStock(line.gameId(), line.quantity(), 0));
			if (inventory.getQuantity() < line.quantity()) {
				throw insufficientStock(line.gameId(), line.quantity(), inventory.getQuantity());
			}
			reservedLines.add(new ReservedLine(game, inventory, line.quantity()));
		}

		reservedLines.forEach(line -> line.inventory().decreaseBy(line.quantity()));
		Order order = orderRepository.saveAndFlush(new Order(user, OrderStatus.PENDING));
		List<OrderLine> savedLines = orderLineRepository.saveAllAndFlush(reservedLines.stream()
				.map(line -> new OrderLine(
						order, line.game(), line.quantity(), line.game().getPrice()))
				.toList());
		return orderMapper.toResponse(order, savedLines);
	}

	@Transactional(readOnly = true)
	public PageResponse<OrderResponse> findCurrentUserOrders(Long userId, int page, int size) {
		validatePage(page, size);
		Page<Order> orders = orderRepository.findAllByUserId(
				userId,
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

	@Transactional(readOnly = true)
	public OrderResponse findCurrentUserOrder(Long userId, Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Order " + orderId + " was not found."));
		if (!order.getUser().getId().equals(userId)) {
			throw new ForbiddenOperationException("Only the order owner may view it.");
		}
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

	private static List<OrderLineRequest> validateAndSortLines(List<OrderLineRequest> requestedLines) {
		if (requestedLines == null || requestedLines.isEmpty() || requestedLines.size() > 100) {
			throw new InvalidRequestException("An order must contain between 1 and 100 lines.");
		}
		Set<Long> gameIds = new HashSet<>();
		for (OrderLineRequest line : requestedLines) {
			if (line == null || line.gameId() == null || line.gameId() < 1
					|| line.quantity() == null || line.quantity() < 1) {
				throw new InvalidRequestException(
						"Every order line requires a positive game identifier and quantity.");
			}
			if (!gameIds.add(line.gameId())) {
				throw new InvalidRequestException("Each game may appear only once in an order.");
			}
		}
		return requestedLines.stream()
				.sorted(Comparator.comparing(OrderLineRequest::gameId))
				.toList();
	}

	private static void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > PAGE_SIZE_MAX) {
			throw new InvalidRequestException(
					"Page must be non-negative and size must be between 1 and " + PAGE_SIZE_MAX + ".");
		}
	}

	private static BusinessRuleViolationException insufficientStock(
			Long gameId,
			int requested,
			int available) {
		return new BusinessRuleViolationException(
				"Insufficient stock for game " + gameId
						+ ": requested " + requested + ", available " + available + ".");
	}

	private record ReservedLine(Game game, Inventory inventory, int quantity) {
	}
}
