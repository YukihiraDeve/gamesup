package com.gamesup.api.order.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;
import com.gamesup.api.order.domain.Inventory;
import com.gamesup.api.order.infrastructure.persistence.InventoryRepository;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerTest {

	private static final String RAW_PASSWORD = "a-secure-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PublisherRepository publisherRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void protectsOwnershipRecalculatesPricesAndRestrictsAdministration() throws Exception {
		User owner = saveUser("order-http-owner@example.com", Role.CLIENT);
		User other = saveUser("order-http-other@example.com", Role.CLIENT);
		User administrator = saveUser("order-http-admin@example.com", Role.ADMIN);
		Game game = saveGame("HTTP ordered game", "19.90", 4);
		String ownerToken = loginToken(owner.getEmail());
		String otherToken = loginToken(other.getEmail());
		String adminToken = loginToken(administrator.getEmail());

		mockMvc.perform(post("/api/v1/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"lines\":[{\"gameId\":%d,\"quantity\":1}]}".formatted(game.getId())))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"lines\":[{\"gameId\":%d,\"quantity\":0}]}".formatted(game.getId())))
				.andExpect(status().isBadRequest());

		MvcResult creation = mockMvc.perform(post("/api/v1/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "lines": [{
						    "gameId": %d,
						    "quantity": 2,
						    "unitPrice": 0.01
						  }]
						}
						""".formatted(game.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.lines[0].unitPrice").value(19.90))
				.andExpect(jsonPath("$.total").value(39.80))
				.andReturn();
		Long orderId = objectMapper.readTree(creation.getResponse().getContentAsByteArray())
				.get("id")
				.longValue();

		mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(owner.getId()));

		mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());

		mockMvc.perform(get("/api/v1/admin/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/admin/orders")
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(orderId));

		mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/status", orderId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"SHIPPED\"}"))
				.andExpect(status().isUnprocessableEntity());

		mockMvc.perform(patch("/api/v1/admin/orders/{orderId}/status", orderId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PAID\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"));
	}

	private User saveUser(String email, Role role) {
		return userRepository.saveAndFlush(new User(
				email,
				passwordEncoder.encode(RAW_PASSWORD),
				"Order",
				"Customer",
				role,
				true));
	}

	private Game saveGame(String name, String price, int quantity) {
		Publisher publisher = publisherRepository.saveAndFlush(new Publisher(name + " publisher"));
		Game game = gameRepository.saveAndFlush(new Game(
				name,
				"A game used by order HTTP tests.",
				new BigDecimal(price),
				2,
				4,
				10,
				45,
				1,
				true,
				publisher,
				Set.of(),
				Set.of()));
		inventoryRepository.saveAndFlush(new Inventory(game, quantity));
		return game;
	}

	private String loginToken(String email) throws Exception {
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

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
