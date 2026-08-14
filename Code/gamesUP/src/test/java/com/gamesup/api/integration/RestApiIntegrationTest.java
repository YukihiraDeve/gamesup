package com.gamesup.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.auth.infrastructure.persistence.UserRepository;
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.order.infrastructure.persistence.InventoryRepository;
import com.gamesup.api.order.infrastructure.persistence.OrderRepository;
import com.gamesup.api.testsupport.DatabaseCleaner;
import com.gamesup.api.testsupport.IntegrationFixtures;
import com.gamesup.api.testsupport.MySqlIntegrationTest;

@SpringBootTest(properties = {
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestApiIntegrationTest extends MySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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

	@Autowired
	private PasswordEncoder passwordEncoder;

	private IntegrationFixtures fixtures;

	@BeforeEach
	void resetDatabase() {
		new DatabaseCleaner(jdbcTemplate).clean();
		fixtures = new IntegrationFixtures(
				userRepository,
				publisherRepository,
				gameRepository,
				inventoryRepository,
				passwordEncoder);
	}

	@Test
	void runsIdentityAdministrationAndCatalogCrudThroughTheFullStack() throws Exception {
		JsonNode firstClient = register("first-client@example.com");
		JsonNode secondClient = register("second-client@example.com");
		String clientToken = login("first-client@example.com");
		var administrator = fixtures.user("admin-catalog-integration@example.com", Role.ADMIN);
		String adminToken = login(administrator.getEmail());

		mockMvc.perform(patch("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"Updated\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(firstClient.get("userId").longValue()))
				.andExpect(jsonPath("$.firstName").value("Updated"));

		mockMvc.perform(get("/api/v1/admin/users")
				.header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/admin/users")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3));
		mockMvc.perform(patch("/api/v1/admin/users/{id}/role", secondClient.get("userId").longValue())
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));

		mockMvc.perform(post("/api/v1/admin/catalog/authors")
				.header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Forbidden Author\"}"))
				.andExpect(status().isForbidden());

		long authorId = createReference(adminToken, "authors", "Integration Author");
		long publisherId = createReference(adminToken, "publishers", "Integration Publisher");
		long categoryId = createReference(adminToken, "categories", "Strategy");
		long unusedCategoryId = createReference(adminToken, "categories", "Unused");
		long gameId = createGame(adminToken, authorId, publisherId, categoryId);

		mockMvc.perform(get("/api/v1/games").param("q", "Integration"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(gameId));
		mockMvc.perform(put("/api/v1/admin/catalog/games/{id}", gameId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(gameJson("Updated Integration Game", "54.90", publisherId, authorId, categoryId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.price").value(54.90));
		mockMvc.perform(delete("/api/v1/admin/catalog/categories/{id}", unusedCategoryId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/admin/catalog/authors/{id}", authorId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isConflict());
		mockMvc.perform(delete("/api/v1/admin/catalog/games/{id}", gameId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/games/{id}", gameId))
				.andExpect(status().isNotFound());
	}

	@Test
	void isolatesWishlistAndReviewsBetweenClientsAndSupportsModeration() throws Exception {
		var owner = fixtures.user("owner-interactions@example.com", Role.CLIENT);
		var other = fixtures.user("other-interactions@example.com", Role.CLIENT);
		var administrator = fixtures.user("admin-interactions@example.com", Role.ADMIN);
		Game game = fixtures.game("Interaction Game", "29.90", 5);
		String ownerToken = login(owner.getEmail());
		String otherToken = login(other.getEmail());
		String adminToken = login(administrator.getEmail());

		mockMvc.perform(put("/api/v1/users/me/wishlist/{gameId}", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.games.length()").value(1));
		mockMvc.perform(put("/api/v1/users/me/wishlist/{gameId}", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.games.length()").value(1));
		mockMvc.perform(get("/api/v1/users/me/wishlist")
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.games").isEmpty());

		MvcResult creation = mockMvc.perform(post("/api/v1/games/{gameId}/reviews", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":5,\"comment\":\"Excellent\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andReturn();
		long reviewId = json(creation).get("id").longValue();

		mockMvc.perform(post("/api/v1/games/{gameId}/reviews", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":4}"))
				.andExpect(status().isConflict());
		mockMvc.perform(put("/api/v1/reviews/{id}", reviewId)
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":1}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/games/{gameId}/reviews", game.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].reviewerName").value("Client"))
				.andExpect(jsonPath("$.content[0].email").doesNotExist());
		mockMvc.perform(patch("/api/v1/admin/reviews/{id}/visibility", reviewId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"hidden\":true}"))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/games/{gameId}/reviews", game.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());
		mockMvc.perform(delete("/api/v1/reviews/{id}", reviewId)
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/users/me/wishlist/{gameId}", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isNoContent());
	}

	@Test
	void rollsBackInvalidOrdersAndEnforcesOwnerAndAdministratorRights() throws Exception {
		var owner = fixtures.user("owner-orders-integration@example.com", Role.CLIENT);
		var other = fixtures.user("other-orders-integration@example.com", Role.CLIENT);
		var administrator = fixtures.user("admin-orders-integration@example.com", Role.ADMIN);
		Game available = fixtures.game("Available Integration Game", "12.50", 5);
		Game insufficient = fixtures.game("Insufficient Integration Game", "30.00", 1);
		String ownerToken = login(owner.getEmail());
		String otherToken = login(other.getEmail());
		String adminToken = login(administrator.getEmail());

		mockMvc.perform(post("/api/v1/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(orderJson(available.getId(), 2, insufficient.getId(), 2)))
				.andExpect(status().isUnprocessableEntity());
		assertThat(orderRepository.count()).isZero();
		assertThat(inventoryRepository.findByGameId(available.getId()).orElseThrow().getQuantity())
				.isEqualTo(5);
		assertThat(inventoryRepository.findByGameId(insufficient.getId()).orElseThrow().getQuantity())
				.isEqualTo(1);

		MvcResult creation = mockMvc.perform(post("/api/v1/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"lines":[{"gameId":%d,"quantity":2,"unitPrice":0.01}]}
						""".formatted(available.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.total").value(25.00))
				.andExpect(jsonPath("$.lines[0].unitPrice").value(12.50))
				.andReturn();
		long orderId = json(creation).get("id").longValue();

		mockMvc.perform(get("/api/v1/orders/{id}", orderId)
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/admin/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/admin/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"SHIPPED\"}"))
				.andExpect(status().isUnprocessableEntity());
		mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PAID\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"));
	}

	private JsonNode register(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email":"%s",
						  "password":"%s",
						  "firstName":"Client",
						  "lastName":"Integration"
						}
						""".formatted(email, IntegrationFixtures.RAW_PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result);
	}

	private String login(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s"}
						""".formatted(email, IntegrationFixtures.RAW_PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return json(result).get("accessToken").asText();
	}

	private long createReference(String token, String resource, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/admin/catalog/{resource}", resource)
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"%s\"}".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result).get("id").longValue();
	}

	private long createGame(
			String token,
			long authorId,
			long publisherId,
			long categoryId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/admin/catalog/games")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(gameJson(
						"Integration Game", "49.90", publisherId, authorId, categoryId)))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result).get("id").longValue();
	}

	private static String gameJson(
			String name,
			String price,
			long publisherId,
			long authorId,
			long categoryId) {
		return """
				{
				  "name":"%s",
				  "description":"A complete integration game",
				  "price":%s,
				  "minPlayers":2,
				  "maxPlayers":5,
				  "minAge":10,
				  "durationMinutes":60,
				  "editionNumber":1,
				  "publisherId":%d,
				  "authorIds":[%d],
				  "categoryIds":[%d]
				}
				""".formatted(name, price, publisherId, authorId, categoryId);
	}

	private static String orderJson(
			long firstGameId,
			int firstQuantity,
			long secondGameId,
			int secondQuantity) {
		return """
				{"lines":[
				  {"gameId":%d,"quantity":%d},
				  {"gameId":%d,"quantity":%d}
				]}
				""".formatted(firstGameId, firstQuantity, secondGameId, secondQuantity);
	}

	private JsonNode json(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsByteArray());
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
