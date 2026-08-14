package com.gamesup.api.testsupport;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.order.domain.Inventory;
import com.gamesup.api.order.infrastructure.persistence.InventoryRepository;

public final class IntegrationFixtures {

	public static final String RAW_PASSWORD = "a-secure-password";

	private final UserRepository userRepository;
	private final PublisherRepository publisherRepository;
	private final GameRepository gameRepository;
	private final InventoryRepository inventoryRepository;
	private final PasswordEncoder passwordEncoder;

	public IntegrationFixtures(
			UserRepository userRepository,
			PublisherRepository publisherRepository,
			GameRepository gameRepository,
			InventoryRepository inventoryRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.publisherRepository = publisherRepository;
		this.gameRepository = gameRepository;
		this.inventoryRepository = inventoryRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public User user(String email, Role role) {
		return userRepository.saveAndFlush(new User(
				email,
				passwordEncoder.encode(RAW_PASSWORD),
				role == Role.ADMIN ? "Admin" : "Client",
				"Integration",
				role,
				true));
	}

	public Game game(String name, String price, int stock) {
		Publisher publisher = publisherRepository.saveAndFlush(new Publisher(name + " publisher"));
		Game game = gameRepository.saveAndFlush(new Game(
				name,
				"A game used by full-stack integration tests.",
				new BigDecimal(price),
				2,
				5,
				10,
				60,
				1,
				true,
				publisher,
				Set.of(),
				Set.of()));
		inventoryRepository.saveAndFlush(new Inventory(game, stock));
		return game;
	}
}
