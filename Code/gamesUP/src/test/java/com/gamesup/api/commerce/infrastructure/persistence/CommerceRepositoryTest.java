package com.gamesup.api.commerce.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.customer.domain.Review;
import com.gamesup.api.customer.domain.Wishlist;
import com.gamesup.api.customer.domain.WishlistItem;
import com.gamesup.api.customer.infrastructure.persistence.ReviewRepository;
import com.gamesup.api.customer.infrastructure.persistence.WishlistItemRepository;
import com.gamesup.api.customer.infrastructure.persistence.WishlistRepository;
import com.gamesup.api.order.domain.Inventory;
import com.gamesup.api.order.domain.Order;
import com.gamesup.api.order.domain.OrderLine;
import com.gamesup.api.order.domain.OrderStatus;
import com.gamesup.api.order.infrastructure.persistence.InventoryRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderLineRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderRepository;

import jakarta.validation.ConstraintViolationException;

@DataJpaTest(properties = {
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
class CommerceRepositoryTest {

	private static final String PASSWORD_HASH =
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5");

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private WishlistItemRepository wishlistItemRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderLineRepository orderLineRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void persistsStockAndUsesOptimisticLocking() {
		Fixture fixture = fixture("inventory@example.com");
		Inventory inventory = inventoryRepository.saveAndFlush(new Inventory(fixture.game(), 5));

		assertThat(inventory.getVersion()).isZero();

		inventory.setQuantity(3);
		inventoryRepository.flush();

		assertThat(inventory.getVersion()).isOne();
		assertThat(inventoryRepository.findByGameId(fixture.game().getId()))
				.hasValueSatisfying(stock -> {
					assertThat(stock.getQuantity()).isEqualTo(3);
					assertThat(stock.getGame().getId()).isEqualTo(fixture.game().getId());
				});
	}

	@Test
	void rejectsNegativeStock() {
		Fixture fixture = fixture("stock-constraint@example.com");

		assertThatThrownBy(() -> inventoryRepository.saveAndFlush(new Inventory(fixture.game(), -1)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void enforcesOneStockPerGame() {
		Fixture fixture = fixture("duplicate-stock@example.com");
		inventoryRepository.saveAndFlush(new Inventory(fixture.game(), 5));

		assertThatThrownBy(() -> inventoryRepository.saveAndFlush(new Inventory(fixture.game(), 8)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void enforcesOneWishlistPerUser() {
		Fixture fixture = fixture("wishlist-owner@example.com");
		wishlistRepository.saveAndFlush(new Wishlist(fixture.user()));

		assertThatThrownBy(() -> wishlistRepository.saveAndFlush(new Wishlist(fixture.user())))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void preventsDuplicateGamesInWishlist() {
		Fixture fixture = fixture("wishlist-item@example.com");
		Wishlist wishlist = wishlistRepository.save(new Wishlist(fixture.user()));
		wishlistItemRepository.saveAndFlush(new WishlistItem(wishlist, fixture.game()));

		assertThat(wishlistItemRepository.existsByWishlistIdAndGameId(
				wishlist.getId(), fixture.game().getId())).isTrue();
		assertThatThrownBy(() -> wishlistItemRepository.saveAndFlush(
				new WishlistItem(wishlist, fixture.game())))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void enforcesOneReviewPerUserAndGame() {
		Fixture fixture = fixture("review-owner@example.com");
		reviewRepository.saveAndFlush(new Review(fixture.user(), fixture.game(), 5, "Excellent"));

		assertThat(reviewRepository.findByUserIdAndGameId(
				fixture.user().getId(), fixture.game().getId()))
				.hasValueSatisfying(review -> {
					assertThat(review.getRating()).isEqualTo(5);
					assertThat(review.getComment()).isEqualTo("Excellent");
				});
		assertThatThrownBy(() -> reviewRepository.saveAndFlush(
				new Review(fixture.user(), fixture.game(), 4, "Duplicate")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsReviewRatingOutsideOneToFive() {
		Fixture fixture = fixture("invalid-review@example.com");

		assertThatThrownBy(() -> reviewRepository.saveAndFlush(
				new Review(fixture.user(), fixture.game(), 6, "Invalid")))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void persistsOrderStatusQuantityAndHistoricalUnitPrice() {
		Fixture fixture = fixture("order-owner@example.com");
		Order order = orderRepository.save(new Order(fixture.user(), OrderStatus.PENDING));
		OrderLine savedLine = orderLineRepository.saveAndFlush(
				new OrderLine(order, fixture.game(), 2, new BigDecimal("29.90")));

		entityManager.clear();

		assertThat(orderRepository.findById(order.getId()))
				.hasValueSatisfying(savedOrder -> {
					assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
					assertThat(savedOrder.getUser().getId()).isEqualTo(fixture.user().getId());
				});
		assertThat(orderLineRepository.findById(savedLine.getId()))
				.hasValueSatisfying(line -> {
					assertThat(line.getQuantity()).isEqualTo(2);
					assertThat(line.getUnitPrice()).isEqualByComparingTo("29.90");
					assertThat(line.getGame().getPrice()).isEqualByComparingTo("39.90");
				});
	}

	private Fixture fixture(String email) {
		User user = userRepository.save(new User(
				email,
				PASSWORD_HASH,
				"Client",
				"GamesUP",
				Role.CLIENT,
				true));
		Publisher publisher = publisherRepository.save(new Publisher("Publisher " + email));
		Game game = gameRepository.save(new Game(
				"Game " + email,
				"A persisted board game",
				new BigDecimal("39.90"),
				2,
				4,
				8,
				45,
				1,
				true,
				publisher,
				Set.of(),
				Set.of()));
		return new Fixture(user, game);
	}

	private record Fixture(User user, Game game) {
	}
}
