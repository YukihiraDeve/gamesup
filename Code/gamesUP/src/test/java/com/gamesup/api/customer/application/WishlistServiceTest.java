package com.gamesup.api.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.application.GameResponseMapper;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;
import com.gamesup.api.customer.infrastructure.persistence.WishlistItemRepository;
import com.gamesup.api.customer.infrastructure.persistence.WishlistRepository;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({WishlistService.class, GameResponseMapper.class})
class WishlistServiceTest {

	@Autowired
	private WishlistService wishlistService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private WishlistItemRepository wishlistItemRepository;

	@Test
	void createsAnIsolatedWishlistAndMakesRepeatedAddsIdempotent() {
		User owner = saveUser("wishlist-owner@example.com");
		User other = saveUser("wishlist-other@example.com");
		Game ownerGame = saveGame("Owner game", true);
		Game otherGame = saveGame("Other game", true);

		wishlistService.addGame(owner.getId(), ownerGame.getId());
		wishlistService.addGame(owner.getId(), ownerGame.getId());
		wishlistService.addGame(other.getId(), otherGame.getId());

		assertThat(wishlistService.findCurrentWishlist(owner.getId()).games())
				.extracting(game -> game.id())
				.containsExactly(ownerGame.getId());
		assertThat(wishlistService.findCurrentWishlist(other.getId()).games())
				.extracting(game -> game.id())
				.containsExactly(otherGame.getId());
		assertThat(wishlistRepository.count()).isEqualTo(2);
		assertThat(wishlistItemRepository.count()).isEqualTo(2);
	}

	@Test
	void refusesInactiveAndMissingGamesWithoutCreatingAWishlist() {
		User owner = saveUser("wishlist-invalid-game@example.com");
		Game inactiveGame = saveGame("Inactive game", false);
		Game activeGame = saveGame("Active game without user", true);

		assertThatThrownBy(() -> wishlistService.addGame(owner.getId(), inactiveGame.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> wishlistService.addGame(owner.getId(), Long.MAX_VALUE))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> wishlistService.addGame(Long.MAX_VALUE, activeGame.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThat(wishlistRepository.count()).isZero();
	}

	@Test
	void ignoresDeletionOfAbsentItemsAndRemovesAnExistingItem() {
		User owner = saveUser("wishlist-delete@example.com");
		Game game = saveGame("Deleted game", true);

		wishlistService.removeGame(owner.getId(), game.getId());
		assertThat(wishlistRepository.count()).isZero();

		wishlistService.addGame(owner.getId(), game.getId());
		wishlistService.removeGame(owner.getId(), Long.MAX_VALUE);
		assertThat(wishlistService.findCurrentWishlist(owner.getId()).games()).hasSize(1);

		wishlistService.removeGame(owner.getId(), game.getId());
		assertThat(wishlistService.findCurrentWishlist(owner.getId()).games()).isEmpty();
	}

	private User saveUser(String email) {
		return userRepository.saveAndFlush(new User(
				email,
				"$2a$10$test-only-password-hash",
				"Wishlist",
				"User",
				Role.CLIENT,
				true));
	}

	private Game saveGame(String name, boolean active) {
		Publisher publisher = publisherRepository.saveAndFlush(new Publisher(name + " publisher"));
		return gameRepository.saveAndFlush(new Game(
				name,
				"A game used by wishlist tests.",
				new BigDecimal("29.90"),
				2,
				4,
				10,
				45,
				1,
				active,
				publisher,
				Set.of(),
				Set.of()));
	}
}
