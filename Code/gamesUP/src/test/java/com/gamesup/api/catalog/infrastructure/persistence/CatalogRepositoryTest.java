package com.gamesup.api.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.gamesup.api.catalog.domain.Author;
import com.gamesup.api.catalog.domain.Category;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.testsupport.MySqlIntegrationTest;

import jakarta.validation.ConstraintViolationException;

@DataJpaTest(properties = {
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CatalogRepositoryTest extends MySqlIntegrationTest {

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void persistsRelationsAndSearchesWithSpecifications() {
		Author author = authorRepository.save(new Author("  Michael   Kiesling "));
		Category category = categoryRepository.save(new Category("Abstract"));
		Publisher publisher = publisherRepository.save(new Publisher("Plan B Games"));
		Game saved = gameRepository.saveAndFlush(game(
				"Azul",
				new BigDecimal("39.90"),
				true,
				publisher,
				author,
				category));
		gameRepository.saveAndFlush(game(
				"Azul Summer Pavilion",
				new BigDecimal("35.90"),
				false,
				publisher,
				author,
				category));

		entityManager.clear();

		assertThat(gameRepository.findById(saved.getId()))
				.hasValueSatisfying(game -> {
					assertThat(game.getPublisher().getName()).isEqualTo("Plan B Games");
					assertThat(game.getAuthors()).extracting(Author::getName)
							.containsExactly("Michael Kiesling");
					assertThat(game.getCategories()).extracting(Category::getName)
							.containsExactly("Abstract");
				});

		assertThat(gameRepository.findAll(
				GameSpecifications.isActive(true)
						.and(GameSpecifications.containsText("KIESLING"))
						.and(GameSpecifications.hasCategory(" abstract "))
						.and(GameSpecifications.hasPriceBetween(
								new BigDecimal("30.00"), new BigDecimal("40.00")))))
				.extracting(Game::getName)
				.containsExactly("Azul");

		assertThat(authorRepository.findByNormalizedName("michael kiesling")).isPresent();
		assertThat(categoryRepository.existsByNormalizedName("abstract")).isTrue();
		assertThat(publisherRepository.findByNormalizedName("plan b games")).isPresent();
	}

	@Test
	void rejectsDuplicateNormalizedNames() {
		authorRepository.saveAndFlush(new Author("Bruno Cathala"));

		assertThatThrownBy(() -> authorRepository.saveAndFlush(new Author("  BRUNO   CATHALA ")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsInvalidGameConstraints() {
		Publisher publisher = publisherRepository.save(new Publisher("Space Cowboys"));

		Game invalidGame = new Game(
				"Invalid",
				"Invalid catalogue entry",
				new BigDecimal("-0.01"),
				4,
				2,
				10,
				30,
				1,
				true,
				publisher,
				Set.of(),
				Set.of());

		assertThatThrownBy(() -> gameRepository.saveAndFlush(invalidGame))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void doesNotCascadeDeletionToReferencedAuthors() {
		Author author = authorRepository.save(new Author("Reiner Knizia"));
		Category category = categoryRepository.save(new Category("Strategy"));
		Publisher publisher = publisherRepository.save(new Publisher("Kosmos"));
		gameRepository.saveAndFlush(game(
				"Tigris & Euphrates",
				new BigDecimal("44.90"),
				true,
				publisher,
				author,
				category));

		authorRepository.delete(author);

		assertThatThrownBy(authorRepository::flush)
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static Game game(
			String name,
			BigDecimal price,
			boolean active,
			Publisher publisher,
			Author author,
			Category category) {
		return new Game(
				name,
				"A modern board game",
				price,
				2,
				4,
				8,
				45,
				1,
				active,
				publisher,
				Set.of(author),
				Set.of(category));
	}
}
