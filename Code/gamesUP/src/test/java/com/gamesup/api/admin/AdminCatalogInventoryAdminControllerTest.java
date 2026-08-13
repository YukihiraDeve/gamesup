package com.gamesup.api.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.domain.User;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Author;
import com.gamesup.api.catalog.domain.Category;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.AuthorRepository;
import com.gamesup.api.catalog.infrastructure.persistence.CategoryRepository;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminCatalogInventoryAdminControllerTest {

	private static final String RAW_PASSWORD = "a-secure-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Test
	void rejectsAdminRoutesWithoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/admin/catalog/authors"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		mockMvc.perform(get("/api/v1/admin/inventory/1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refusesAdminRoutesToClient() throws Exception {
		String token = loginToken("client-admin-routes@example.com", Role.CLIENT);

		mockMvc.perform(get("/api/v1/admin/catalog/authors")
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/v1/admin/inventory/1")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":5}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void allowsAdminToManageReferencesAndReportsValidationAndConflicts() throws Exception {
		String token = loginToken("admin-references@example.com", Role.ADMIN);

		mockMvc.perform(post("/api/v1/admin/catalog/authors")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Bruno Cathala\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Bruno Cathala"));

		mockMvc.perform(post("/api/v1/admin/catalog/authors")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"  bruno   cathala  \"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/admin/catalog/categories")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void allowsAdminGameLifecycleAndHidesArchivedGameFromPublicCatalog() throws Exception {
		String token = loginToken("admin-games@example.com", Role.ADMIN);
		Author author = authorRepository.save(new Author("Game Author"));
		Category category = categoryRepository.save(new Category("Strategy"));
		Publisher publisher = publisherRepository.save(new Publisher("Game Publisher"));
		String invalidPlayerRange = gameJson(
				"Invalid Game",
				publisher.getId(),
				author.getId(),
				category.getId()).replace("\"maxPlayers\": 4", "\"maxPlayers\": 1");

		mockMvc.perform(post("/api/v1/admin/catalog/games")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidPlayerRange))
				.andExpect(status().isBadRequest());

		MvcResult createdResult = mockMvc.perform(post("/api/v1/admin/catalog/games")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(gameJson("Created Game", publisher.getId(), author.getId(), category.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.active").value(true))
				.andReturn();
		Long gameId = objectMapper.readTree(createdResult.getResponse().getContentAsByteArray()).get("id").asLong();

		mockMvc.perform(get("/api/v1/admin/catalog/games/{id}", gameId)
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Created Game"));

		mockMvc.perform(put("/api/v1/admin/catalog/games/{id}", gameId)
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(gameJson("Updated Game", publisher.getId(), author.getId(), category.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Game"));

		mockMvc.perform(delete("/api/v1/admin/catalog/games/{id}", gameId)
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/games/{id}", gameId))
				.andExpect(status().isNotFound());
	}

	@Test
	void allowsAdminAbsoluteInventoryAdjustmentAndRejectsInvalidPayload() throws Exception {
		String token = loginToken("admin-inventory@example.com", Role.ADMIN);
		Game game = saveGame();

		mockMvc.perform(put("/api/v1/admin/inventory/{gameId}", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":7}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gameId").value(game.getId()))
				.andExpect(jsonPath("$.quantity").value(7));

		mockMvc.perform(get("/api/v1/admin/inventory/{gameId}", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(7));

		mockMvc.perform(put("/api/v1/admin/inventory/{gameId}", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":-1}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(put("/api/v1/admin/inventory/{gameId}", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	private String loginToken(String email, Role role) throws Exception {
		userRepository.saveAndFlush(new User(
				email,
				passwordEncoder.encode(RAW_PASSWORD),
				"Test",
				"Admin",
				role,
				true));
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "password": "%s"
						}
						""".formatted(email, RAW_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		return response.get("accessToken").asText();
	}

	private Game saveGame() {
		Author author = authorRepository.save(new Author("Stock Author"));
		Category category = categoryRepository.save(new Category("Stock Category"));
		Publisher publisher = publisherRepository.save(new Publisher("Stock Publisher"));
		return gameRepository.saveAndFlush(new Game(
				"Stock Game",
				"Game used for the stock endpoint.",
				new BigDecimal("24.90"),
				1,
				5,
				8,
				30,
				1,
				true,
				publisher,
				Set.of(author),
				Set.of(category)));
	}

	private static String gameJson(String name, Long publisherId, Long authorId, Long categoryId) {
		return """
				{
				  "name": "%s",
				  "description": "A complete game description.",
				  "price": 39.90,
				  "minPlayers": 2,
				  "maxPlayers": 4,
				  "minAge": 8,
				  "durationMinutes": 45,
				  "editionNumber": 1,
				  "publisherId": %d,
				  "authorIds": [%d],
				  "categoryIds": [%d]
				}
				""".formatted(name, publisherId, authorId, categoryId);
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
