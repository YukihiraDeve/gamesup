package com.gamesup.api.customer.web;

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
import com.gamesup.api.catalog.domain.Game;
import com.gamesup.api.catalog.domain.Publisher;
import com.gamesup.api.catalog.infrastructure.persistence.GameRepository;
import com.gamesup.api.catalog.infrastructure.persistence.PublisherRepository;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WishlistControllerTest {

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
	private PasswordEncoder passwordEncoder;

	@Test
	void requiresAClientAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/wishlist"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		User administrator = saveUser("wishlist-admin@example.com", Role.ADMIN);
		mockMvc.perform(get("/api/v1/users/me/wishlist")
				.header(HttpHeaders.AUTHORIZATION, bearer(loginToken(administrator.getEmail()))))
				.andExpect(status().isForbidden());
	}

	@Test
	void isolatesAccountsAndMakesDuplicateAddsIdempotent() throws Exception {
		User owner = saveUser("wishlist-http-owner@example.com", Role.CLIENT);
		User other = saveUser("wishlist-http-other@example.com", Role.CLIENT);
		Game ownerGame = saveGame("HTTP owner game", true);
		Game otherGame = saveGame("HTTP other game", true);
		String ownerToken = loginToken(owner.getEmail());
		String otherToken = loginToken(other.getEmail());

		mockMvc.perform(put("/api/v1/users/me/wishlist/{gameId}", ownerGame.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.games.length()").value(1))
				.andExpect(jsonPath("$.games[0].id").value(ownerGame.getId()))
				.andExpect(jsonPath("$.games[0].description").doesNotExist())
				.andExpect(jsonPath("$.games[0].active").doesNotExist());

		mockMvc.perform(put("/api/v1/users/me/wishlist/{gameId}", ownerGame.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.games.length()").value(1));

		mockMvc.perform(put("/api/v1/users/me/wishlist/{gameId}", otherGame.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/users/me/wishlist")
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.games.length()").value(1))
				.andExpect(jsonPath("$.games[0].id").value(ownerGame.getId()));
	}

	@Test
	void refusesInactiveGamesAndTreatsAbsentDeletionAsSuccessful() throws Exception {
		User owner = saveUser("wishlist-http-delete@example.com", Role.CLIENT);
		Game inactiveGame = saveGame("HTTP inactive game", false);
		String token = loginToken(owner.getEmail());

		mockMvc.perform(put("/api/v1/users/me/wishlist/{gameId}", inactiveGame.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		mockMvc.perform(delete("/api/v1/users/me/wishlist/{gameId}", Long.MAX_VALUE)
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/users/me/wishlist")
				.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.games").isEmpty());
	}

	private User saveUser(String email, Role role) {
		return userRepository.saveAndFlush(new User(
				email,
				passwordEncoder.encode(RAW_PASSWORD),
				"Wishlist",
				"User",
				role,
				true));
	}

	private Game saveGame(String name, boolean active) {
		Publisher publisher = publisherRepository.saveAndFlush(new Publisher(name + " publisher"));
		return gameRepository.saveAndFlush(new Game(
				name,
				"A game used by wishlist HTTP tests.",
				new BigDecimal("39.90"),
				2,
				5,
				12,
				60,
				1,
				active,
				publisher,
				Set.of(),
				Set.of()));
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
