package com.gamesup.api.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.common.application.exception.BusinessRuleViolationException;
import com.gamesup.api.common.application.exception.ForbiddenOperationException;
import com.gamesup.api.order.domain.Inventory;
import com.gamesup.api.order.domain.OrderStatus;
import com.gamesup.api.order.infrastructure.persistence.InventoryRepository;
import com.gamesup.api.order.web.dto.OrderLineRequest;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({OrderService.class, AdminOrderService.class, OrderMapper.class})
class OrderServiceTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private AdminOrderService adminOrderService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Test
	void calculatesPricesOnTheServerAndRestrictsOrderOwnership() {
		User owner = saveUser("order-owner@example.com");
		User other = saveUser("order-other@example.com");
		Game game = saveGame("Calculated order game", "12.50", 10);

		var created = orderService.create(
				owner.getId(), List.of(new OrderLineRequest(game.getId(), 3)));

		assertThat(created.status()).isEqualTo(OrderStatus.PENDING);
		assertThat(created.lines()).hasSize(1);
		assertThat(created.lines().getFirst().unitPrice()).isEqualByComparingTo("12.50");
		assertThat(created.lines().getFirst().lineTotal()).isEqualByComparingTo("37.50");
		assertThat(created.total()).isEqualByComparingTo("37.50");
		assertThat(inventoryRepository.findByGameId(game.getId()).orElseThrow().getQuantity())
				.isEqualTo(7);
		assertThat(orderService.findCurrentUserOrders(owner.getId(), 0, 20).content()).hasSize(1);
		assertThat(orderService.findCurrentUserOrders(other.getId(), 0, 20).content()).isEmpty();
		assertThatThrownBy(() -> orderService.findCurrentUserOrder(other.getId(), created.id()))
				.isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	void appliesOnlyExplicitOrderStatusTransitions() {
		User owner = saveUser("order-status@example.com");
		Game game = saveGame("Status order game", "20.00", 2);
		var created = orderService.create(
				owner.getId(), List.of(new OrderLineRequest(game.getId(), 1)));

		assertThatThrownBy(() -> adminOrderService.transition(created.id(), OrderStatus.SHIPPED))
				.isInstanceOf(BusinessRuleViolationException.class);
		assertThat(adminOrderService.transition(created.id(), OrderStatus.PAID).status())
				.isEqualTo(OrderStatus.PAID);
		assertThat(adminOrderService.transition(created.id(), OrderStatus.SHIPPED).status())
				.isEqualTo(OrderStatus.SHIPPED);
		assertThat(adminOrderService.transition(created.id(), OrderStatus.DELIVERED).status())
				.isEqualTo(OrderStatus.DELIVERED);
		assertThat(adminOrderService.transition(created.id(), OrderStatus.ARCHIVED).status())
				.isEqualTo(OrderStatus.ARCHIVED);
		assertThatThrownBy(() -> adminOrderService.transition(created.id(), OrderStatus.PAID))
				.isInstanceOf(BusinessRuleViolationException.class);
	}

	private User saveUser(String email) {
		return userRepository.saveAndFlush(new User(
				email,
				"$2a$10$test-only-password-hash",
				"Order",
				"Customer",
				Role.CLIENT,
				true));
	}

	private Game saveGame(String name, String price, int quantity) {
		Publisher publisher = publisherRepository.saveAndFlush(new Publisher(name + " publisher"));
		Game game = gameRepository.saveAndFlush(new Game(
				name,
				"A game used by order service tests.",
				new BigDecimal(price),
				2,
				4,
				10,
				45,
				1,
				true,
				publisher,
				Set.of(),
				Set.of()));
		inventoryRepository.saveAndFlush(new Inventory(game, quantity));
		return game;
	}
}
