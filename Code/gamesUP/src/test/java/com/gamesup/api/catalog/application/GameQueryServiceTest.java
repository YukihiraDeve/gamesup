package com.gamesup.api.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
import com.gamesup.api.common.web.dto.PageResponse;

@DataJpaTest(properties = {
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@Import({GameQueryService.class, GameResponseMapper.class})
class GameQueryServiceTest {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5");

	@Autowired
	private GameQueryService gameQueryService;

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Test
	void combinesSearchFiltersPaginationAndDeterministicSorting() {
		Author author = authorRepository.save(new Author("Bruno Cathala"));
		Category strategy = categoryRepository.save(new Category("Strategy"));
		Category family = categoryRepository.save(new Category("Family"));
		Publisher publisher = publisherRepository.save(new Publisher("Matagot"));
		saveGame("Cyclades", "Mythological strategy", "49.90", true, publisher, author, strategy);
		saveGame("Five Tribes", "Tactical strategy", "44.90", true, publisher, author, strategy);
		saveGame("Hidden Game", "Inactive strategy", "59.90", false, publisher, author, strategy);
		saveGame("Family Game", "Wrong category", "45.90", true, publisher, author, family);

		PageResponse<?> firstPage = gameQueryService.search(new GameSearchCriteria(
				"  CATHALA ",
				" strategy ",
				new BigDecimal("40.00"),
				new BigDecimal("50.00"),
				0,
				1,
				"price,desc"));
		PageResponse<?> secondPage = gameQueryService.search(new GameSearchCriteria(
				"cathala",
				"strategy",
				new BigDecimal("40.00"),
				new BigDecimal("50.00"),
				1,
				1,
				"price,desc"));

		assertThat(firstPage.content())
				.extracting("name")
				.containsExactly("Cyclades");
		assertThat(firstPage.totalElements()).isEqualTo(2);
		assertThat(firstPage.totalPages()).isEqualTo(2);
		assertThat(secondPage.content())
				.extracting("name")
				.containsExactly("Five Tribes");
	}

	@Test
	void returnsActiveGameDetailsWithOrderedNames() {
		Author secondAuthor = authorRepository.save(new Author("Zoe Author"));
		Author firstAuthor = authorRepository.save(new Author("Alice Author"));
		Category secondCategory = categoryRepository.save(new Category("Strategy"));
		Category firstCategory = categoryRepository.save(new Category("Abstract"));
		Publisher publisher = publisherRepository.save(new Publisher("Plan B Games"));
		Game game = gameRepository.saveAndFlush(new Game(
				"Azul",
				"Decorate the palace walls.",
				new BigDecimal("39.90"),
				2,
				4,
				8,
				45,
				2,
				true,
				publisher,
				Set.of(secondAuthor, firstAuthor),
				Set.of(secondCategory, firstCategory)));

		var response = gameQueryService.findActiveById(game.getId());

		assertThat(response.name()).isEqualTo("Azul");
		assertThat(response.description()).isEqualTo("Decorate the palace walls.");
		assertThat(response.editionNumber()).isEqualTo(2);
		assertThat(response.authors()).containsExactly("Alice Author", "Zoe Author");
		assertThat(response.categories()).containsExactly("Abstract", "Strategy");
	}

	@Test
	void treatsInactiveGameAsNotFound() {
		Author author = authorRepository.save(new Author("Inactive Author"));
		Category category = categoryRepository.save(new Category("Inactive Category"));
		Publisher publisher = publisherRepository.save(new Publisher("Inactive Publisher"));
		Game game = saveGame("Archived", "No longer sold", "19.90", false, publisher, author, category);

		assertThatThrownBy(() -> gameQueryService.findActiveById(game.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void rejectsUnknownSortAndInvertedPriceRange() {
		assertThatThrownBy(() -> gameQueryService.search(criteria("publisher,asc")))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("Sort");

		assertThatThrownBy(() -> gameQueryService.search(new GameSearchCriteria(
				null,
				null,
				new BigDecimal("50.00"),
				new BigDecimal("10.00"),
				0,
				20,
				"name,asc")))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("Minimum price");
	}

	private static GameSearchCriteria criteria(String sort) {
		return new GameSearchCriteria(null, null, null, null, 0, 20, sort);
	}

	private Game saveGame(
			String name,
			String description,
			String price,
			boolean active,
			Publisher publisher,
			Author author,
			Category category) {
		return gameRepository.saveAndFlush(new Game(
				name,
				description,
				new BigDecimal(price),
				2,
				5,
				10,
				60,
				1,
				active,
				publisher,
				Set.of(author),
				Set.of(category)));
	}
}
