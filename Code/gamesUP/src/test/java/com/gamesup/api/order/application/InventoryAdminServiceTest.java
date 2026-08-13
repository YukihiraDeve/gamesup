package com.gamesup.api.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gamesup.api.catalog.domain.Author;
import com.gamesup.api.catalog.domain.Category;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.AuthorRepository;
import com.gamesup.api.catalog.infrastructure.persistence.CategoryRepository;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.common.application.exception.InvalidRequestException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import(InventoryAdminService.class)
class InventoryAdminServiceTest {

	@Autowired
	private InventoryAdminService inventoryAdminService;

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Test
	void createsThenReplacesAbsoluteQuantity() {
		Game game = saveGame();

		var created = inventoryAdminService.setAbsoluteQuantity(game.getId(), 12);
		var updated = inventoryAdminService.setAbsoluteQuantity(game.getId(), 3);

		assertThat(created.quantity()).isEqualTo(12);
		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.quantity()).isEqualTo(3);
		assertThat(updated.version()).isGreaterThan(created.version());
		assertThat(inventoryAdminService.findByGameId(game.getId()).quantity()).isEqualTo(3);
	}

	@Test
	void rejectsNegativeQuantityAndUnknownGame() {
		Game game = saveGame();

		assertThatThrownBy(() -> inventoryAdminService.setAbsoluteQuantity(game.getId(), -1))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> inventoryAdminService.setAbsoluteQuantity(999_999L, 4))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private Game saveGame() {
		Author author = authorRepository.save(new Author("Inventory Author"));
		Category category = categoryRepository.save(new Category("Inventory Category"));
		Publisher publisher = publisherRepository.save(new Publisher("Inventory Publisher"));
		return gameRepository.saveAndFlush(new Game(
				"Inventory Game",
				"Game used for inventory tests.",
				new BigDecimal("29.90"),
				1,
				4,
				8,
				30,
				1,
				true,
				publisher,
				Set.of(author),
				Set.of(category)));
	}
}
