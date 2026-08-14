package com.gamesup.api.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.common.application.exception.BusinessRuleViolationException;
import com.gamesup.api.order.domain.Inventory;
import com.gamesup.api.order.infrastructure.persistence.InventoryRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderRepository;
import com.gamesup.api.order.web.dto.OrderLineRequest;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class InventoryTransactionTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void rollsBackTheWholeOrderWhenOneLineHasInsufficientStock() {
		User user = saveUser("rollback-order@example.com");
		Game availableGame = saveGame("Rollback available game", 5);
		Game unavailableGame = saveGame("Rollback unavailable game", 1);
		long ordersBefore = orderRepository.count();

		assertThatThrownBy(() -> orderService.create(user.getId(), List.of(
				new OrderLineRequest(availableGame.getId(), 2),
				new OrderLineRequest(unavailableGame.getId(), 2))))
				.isInstanceOf(BusinessRuleViolationException.class);

		assertThat(inventoryRepository.findByGameId(availableGame.getId()).orElseThrow().getQuantity())
				.isEqualTo(5);
		assertThat(inventoryRepository.findByGameId(unavailableGame.getId()).orElseThrow().getQuantity())
				.isEqualTo(1);
		assertThat(orderRepository.count()).isEqualTo(ordersBefore);
	}

	@Test
	void serializesConcurrentOrdersSoStockNeverBecomesNegative() throws Exception {
		User firstUser = saveUser("concurrent-order-first@example.com");
		User secondUser = saveUser("concurrent-order-second@example.com");
		Game game = saveGame("Concurrent order game", 1);
		long ordersBefore = orderRepository.count();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			CompletableFuture<Boolean> first = attemptOrder(executor, start, firstUser.getId(), game.getId());
			CompletableFuture<Boolean> second = attemptOrder(executor, start, secondUser.getId(), game.getId());
			start.countDown();

			assertThat(List.of(
					first.get(20, TimeUnit.SECONDS),
					second.get(20, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder(true, false);
		} finally {
			executor.shutdownNow();
		}

		assertThat(inventoryRepository.findByGameId(game.getId()).orElseThrow().getQuantity())
				.isZero();
		assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
	}

	private CompletableFuture<Boolean> attemptOrder(
			ExecutorService executor,
			CountDownLatch start,
			Long userId,
			Long gameId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				start.await();
				orderService.create(userId, List.of(new OrderLineRequest(gameId, 1)));
				return true;
			} catch (BusinessRuleViolationException exception) {
				return false;
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Concurrent order test was interrupted.", exception);
			}
		}, executor);
	}

	private User saveUser(String email) {
		return userRepository.saveAndFlush(new User(
				email,
				"$2a$10$test-only-password-hash",
				"Concurrent",
				"Customer",
				Role.CLIENT,
				true));
	}

	private Game saveGame(String name, int quantity) {
		Publisher publisher = publisherRepository.saveAndFlush(new Publisher(name + " publisher"));
		Game game = gameRepository.saveAndFlush(new Game(
				name,
				"A game used by order transaction tests.",
				new BigDecimal("15.00"),
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
