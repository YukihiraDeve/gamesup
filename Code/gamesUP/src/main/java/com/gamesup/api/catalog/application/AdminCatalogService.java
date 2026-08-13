package com.gamesup.api.catalog.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesup.api.catalog.domain.Author;
import com.gamesup.api.catalog.domain.CatalogNameNormalizer;
import com.gamesup.api.catalog.domain.Category;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.NamedCatalogEntity;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.AuthorRepository;
import com.gamesup.api.catalog.infrastructure.persistence.CategoryRepository;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.catalog.web.admin.dto.AdminGameCreateRequest;
import com.gamesup.api.catalog.web.admin.dto.AdminGameResponse;
import com.gamesup.api.catalog.web.admin.dto.AdminGameUpdateRequest;
import com.gamesup.api.catalog.web.admin.dto.CatalogReferenceResponse;
import com.gamesup.api.common.application.exception.ConflictException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;

@Service
public class AdminCatalogService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminCatalogService.class);
	private static final Sort NAME_SORT = Sort.by("name").ascending().and(Sort.by("id"));

	private final GameRepository gameRepository;
	private final AuthorRepository authorRepository;
	private final PublisherRepository publisherRepository;
	private final CategoryRepository categoryRepository;
	private final AdminCatalogMapper mapper;

	public AdminCatalogService(
			GameRepository gameRepository,
			AuthorRepository authorRepository,
			PublisherRepository publisherRepository,
			CategoryRepository categoryRepository,
			AdminCatalogMapper mapper) {
		this.gameRepository = gameRepository;
		this.authorRepository = authorRepository;
		this.publisherRepository = publisherRepository;
		this.categoryRepository = categoryRepository;
		this.mapper = mapper;
	}

	@Transactional
	public AdminGameResponse createGame(AdminGameCreateRequest request) {
		Publisher publisher = findPublisher(request.publisherId());
		Set<Author> authors = findAuthors(request.authorIds());
		Set<Category> categories = findCategories(request.categoryIds());
		Game game = new Game(
				request.name(),
				request.description(),
				request.price(),
				request.minPlayers(),
				request.maxPlayers(),
				request.minAge(),
				request.durationMinutes(),
				request.editionNumber(),
				true,
				publisher,
				authors,
				categories);
		Game saved = gameRepository.saveAndFlush(game);
		LOGGER.info("Catalog game created: gameId={}", saved.getId());
		return mapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public AdminGameResponse findGame(Long id) {
		return mapper.toResponse(findGameEntity(id));
	}

	@Transactional
	public AdminGameResponse updateGame(Long id, AdminGameUpdateRequest request) {
		Game game = findGameEntity(id);
		game.update(
				request.name(),
				request.description(),
				request.price(),
				request.minPlayers(),
				request.maxPlayers(),
				request.minAge(),
				request.durationMinutes(),
				request.editionNumber(),
				findPublisher(request.publisherId()),
				findAuthors(request.authorIds()),
				findCategories(request.categoryIds()));
		gameRepository.flush();
		LOGGER.info("Catalog game updated: gameId={}", game.getId());
		return mapper.toResponse(game);
	}

	@Transactional
	public void archiveGame(Long id) {
		Game game = findGameEntity(id);
		game.archive();
		gameRepository.flush();
		LOGGER.info("Catalog game archived: gameId={}", game.getId());
	}

	@Transactional(readOnly = true)
	public List<CatalogReferenceResponse> findAuthors() {
		return mapper.toReferences(authorRepository.findAll(NAME_SORT));
	}

	@Transactional
	public CatalogReferenceResponse createAuthor(String name) {
		ensureNameAvailable(name, authorRepository.findByNormalizedName(normalized(name)), null, "Author");
		Author saved = authorRepository.saveAndFlush(new Author(name));
		LOGGER.info("Catalog author created: authorId={}", saved.getId());
		return mapper.toReference(saved);
	}

	@Transactional
	public CatalogReferenceResponse updateAuthor(Long id, String name) {
		Author author = authorRepository.findById(id)
				.orElseThrow(() -> notFound("Author", id));
		ensureNameAvailable(name, authorRepository.findByNormalizedName(normalized(name)), id, "Author");
		author.rename(name);
		authorRepository.flush();
		LOGGER.info("Catalog author updated: authorId={}", id);
		return mapper.toReference(author);
	}

	@Transactional
	public void deleteAuthor(Long id) {
		Author author = authorRepository.findById(id)
				.orElseThrow(() -> notFound("Author", id));
		if (gameRepository.existsByAuthorsId(id)) {
			throw new ConflictException("Author " + id + " is still used by a game.");
		}
		authorRepository.delete(author);
		authorRepository.flush();
		LOGGER.info("Catalog author deleted: authorId={}", id);
	}

	@Transactional(readOnly = true)
	public List<CatalogReferenceResponse> findPublishers() {
		return mapper.toReferences(publisherRepository.findAll(NAME_SORT));
	}

	@Transactional
	public CatalogReferenceResponse createPublisher(String name) {
		ensureNameAvailable(name, publisherRepository.findByNormalizedName(normalized(name)), null, "Publisher");
		Publisher saved = publisherRepository.saveAndFlush(new Publisher(name));
		LOGGER.info("Catalog publisher created: publisherId={}", saved.getId());
		return mapper.toReference(saved);
	}

	@Transactional
	public CatalogReferenceResponse updatePublisher(Long id, String name) {
		Publisher publisher = findPublisher(id);
		ensureNameAvailable(name, publisherRepository.findByNormalizedName(normalized(name)), id, "Publisher");
		publisher.rename(name);
		publisherRepository.flush();
		LOGGER.info("Catalog publisher updated: publisherId={}", id);
		return mapper.toReference(publisher);
	}

	@Transactional
	public void deletePublisher(Long id) {
		Publisher publisher = findPublisher(id);
		if (gameRepository.existsByPublisherId(id)) {
			throw new ConflictException("Publisher " + id + " is still used by a game.");
		}
		publisherRepository.delete(publisher);
		publisherRepository.flush();
		LOGGER.info("Catalog publisher deleted: publisherId={}", id);
	}

	@Transactional(readOnly = true)
	public List<CatalogReferenceResponse> findCategories() {
		return mapper.toReferences(categoryRepository.findAll(NAME_SORT));
	}

	@Transactional
	public CatalogReferenceResponse createCategory(String name) {
		ensureNameAvailable(name, categoryRepository.findByNormalizedName(normalized(name)), null, "Category");
		Category saved = categoryRepository.saveAndFlush(new Category(name));
		LOGGER.info("Catalog category created: categoryId={}", saved.getId());
		return mapper.toReference(saved);
	}

	@Transactional
	public CatalogReferenceResponse updateCategory(Long id, String name) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> notFound("Category", id));
		ensureNameAvailable(name, categoryRepository.findByNormalizedName(normalized(name)), id, "Category");
		category.rename(name);
		categoryRepository.flush();
		LOGGER.info("Catalog category updated: categoryId={}", id);
		return mapper.toReference(category);
	}

	@Transactional
	public void deleteCategory(Long id) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> notFound("Category", id));
		if (gameRepository.existsByCategoriesId(id)) {
			throw new ConflictException("Category " + id + " is still used by a game.");
		}
		categoryRepository.delete(category);
		categoryRepository.flush();
		LOGGER.info("Catalog category deleted: categoryId={}", id);
	}

	private Game findGameEntity(Long id) {
		return gameRepository.findById(id).orElseThrow(() -> notFound("Game", id));
	}

	private Publisher findPublisher(Long id) {
		return publisherRepository.findById(id).orElseThrow(() -> notFound("Publisher", id));
	}

	private Set<Author> findAuthors(Set<Long> ids) {
		List<Author> authors = authorRepository.findAllById(ids);
		if (authors.size() != ids.size()) {
			throw new ResourceNotFoundException("One or more authors were not found.");
		}
		return new LinkedHashSet<>(authors);
	}

	private Set<Category> findCategories(Set<Long> ids) {
		List<Category> categories = categoryRepository.findAllById(ids);
		if (categories.size() != ids.size()) {
			throw new ResourceNotFoundException("One or more categories were not found.");
		}
		return new LinkedHashSet<>(categories);
	}

	private static String normalized(String name) {
		return CatalogNameNormalizer.normalizedName(name);
	}

	private static void ensureNameAvailable(
			String name,
			java.util.Optional<? extends NamedCatalogEntity> existing,
			Long currentId,
			String resource) {
		if (existing.isPresent() && !existing.get().getId().equals(currentId)) {
			throw new ConflictException(resource + " name already exists: " + name.trim() + ".");
		}
	}

	private static ResourceNotFoundException notFound(String resource, Long id) {
		return new ResourceNotFoundException(resource + " " + id + " was not found.");
	}
}
