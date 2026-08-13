package com.gamesup.api.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gamesup.api.catalog.web.admin.dto.AdminGameCreateRequest;
import com.gamesup.api.catalog.web.admin.dto.AdminGameUpdateRequest;
import com.gamesup.api.common.application.exception.ConflictException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({AdminCatalogService.class, AdminCatalogMapper.class, GameQueryService.class, GameResponseMapper.class})
class AdminCatalogServiceTest {

	@Autowired
	private AdminCatalogService adminCatalogService;

	@Autowired
	private GameQueryService gameQueryService;

	@Test
	void createsReadsUpdatesAndArchivesGame() {
		var firstPublisher = adminCatalogService.createPublisher("First Publisher");
		var secondPublisher = adminCatalogService.createPublisher("Second Publisher");
		var firstAuthor = adminCatalogService.createAuthor("First Author");
		var secondAuthor = adminCatalogService.createAuthor("Second Author");
		var firstCategory = adminCatalogService.createCategory("Family");
		var secondCategory = adminCatalogService.createCategory("Strategy");

		var created = adminCatalogService.createGame(createRequest(
				"Original Game",
				firstPublisher.id(),
				firstAuthor.id(),
				firstCategory.id()));

		assertThat(created.active()).isTrue();
		assertThat(adminCatalogService.findGame(created.id()).name()).isEqualTo("Original Game");
		assertThat(gameQueryService.findActiveById(created.id()).name()).isEqualTo("Original Game");

		var updated = adminCatalogService.updateGame(created.id(), new AdminGameUpdateRequest(
				"Updated Game",
				"Updated description",
				new BigDecimal("59.90"),
				2,
				6,
				12,
				90,
				2,
				secondPublisher.id(),
				Set.of(secondAuthor.id()),
				Set.of(secondCategory.id())));

		assertThat(updated.name()).isEqualTo("Updated Game");
		assertThat(updated.publisher().id()).isEqualTo(secondPublisher.id());
		assertThat(updated.authors()).extracting("id").containsExactly(secondAuthor.id());
		assertThat(updated.categories()).extracting("id").containsExactly(secondCategory.id());

		adminCatalogService.archiveGame(created.id());

		assertThat(adminCatalogService.findGame(created.id()).active()).isFalse();
		assertThatThrownBy(() -> gameQueryService.findActiveById(created.id()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void refusesDuplicateReferenceNamesAfterNormalization() {
		adminCatalogService.createAuthor("Bruno Cathala");

		assertThatThrownBy(() -> adminCatalogService.createAuthor("  bruno   cathala "))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void refusesDeletingReferencesUsedByAGame() {
		var publisher = adminCatalogService.createPublisher("Publisher");
		var author = adminCatalogService.createAuthor("Author");
		var category = adminCatalogService.createCategory("Category");
		adminCatalogService.createGame(createRequest(
				"Used References",
				publisher.id(),
				author.id(),
				category.id()));

		assertThatThrownBy(() -> adminCatalogService.deletePublisher(publisher.id()))
				.isInstanceOf(ConflictException.class);
		assertThatThrownBy(() -> adminCatalogService.deleteAuthor(author.id()))
				.isInstanceOf(ConflictException.class);
		assertThatThrownBy(() -> adminCatalogService.deleteCategory(category.id()))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void deletesUnusedReferenceAndRejectsUnknownRelations() {
		var unused = adminCatalogService.createCategory("Unused");
		adminCatalogService.deleteCategory(unused.id());

		assertThat(adminCatalogService.findCategories()).isEmpty();
		assertThatThrownBy(() -> adminCatalogService.createGame(createRequest(
				"Unknown Relations",
				999_999L,
				888_888L,
				777_777L)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private static AdminGameCreateRequest createRequest(
			String name,
			Long publisherId,
			Long authorId,
			Long categoryId) {
		return new AdminGameCreateRequest(
				name,
				"A complete game description.",
				new BigDecimal("39.90"),
				2,
				4,
				8,
				45,
				1,
				publisherId,
				Set.of(authorId),
				Set.of(categoryId));
	}
}
