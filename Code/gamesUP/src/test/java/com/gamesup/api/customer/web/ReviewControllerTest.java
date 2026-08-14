package com.gamesup.api.customer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.gamesup.api.customer.infrastructure.persistence.ReviewRepository;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewControllerTest {

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
	private ReviewRepository reviewRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void supportsOwnershipValidationDuplicatesAndModeration() throws Exception {
		User owner = saveUser("review-http-owner@example.com", "Owner", Role.CLIENT);
		User other = saveUser("review-http-other@example.com", "Other", Role.CLIENT);
		User administrator = saveUser("review-http-admin@example.com", "Admin", Role.ADMIN);
		Game game = saveGame("HTTP reviewed game");
		String ownerToken = loginToken(owner.getEmail());
		String otherToken = loginToken(other.getEmail());
		String adminToken = loginToken(administrator.getEmail());

		MvcResult creation = mockMvc.perform(post("/api/v1/games/{gameId}/reviews", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":4,\"comment\":\"A useful review\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reviewerName").value("Owner"))
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.user").doesNotExist())
				.andReturn();
		Long reviewId = objectMapper.readTree(creation.getResponse().getContentAsByteArray())
				.get("id")
				.longValue();

		mockMvc.perform(post("/api/v1/games/{gameId}/reviews", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":5}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/games/{gameId}/reviews", game.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":3.5}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(put("/api/v1/reviews/{reviewId}", reviewId)
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":1,\"comment\":\"Not mine\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
				.header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/v1/reviews/{reviewId}", Long.MAX_VALUE)
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":5}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/v1/admin/reviews/{reviewId}/visibility", reviewId)
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"hidden\":true}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/admin/reviews/{reviewId}/visibility", reviewId)
				.header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"hidden\":true}"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/games/{gameId}/reviews", game.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());
		assertThat(reviewRepository.findById(reviewId)).isPresent();

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
				.header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/games/{gameId}/reviews", Long.MAX_VALUE))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	private User saveUser(String email, String firstName, Role role) {
		return userRepository.saveAndFlush(new User(
				email,
				passwordEncoder.encode(RAW_PASSWORD),
				firstName,
				"Reviewer",
				role,
				true));
	}

	private Game saveGame(String name) {
		Publisher publisher = publisherRepository.saveAndFlush(new Publisher(name + " publisher"));
		return gameRepository.saveAndFlush(new Game(
				name,
				"A game used by review HTTP tests.",
				new BigDecimal("49.90"),
				2,
				5,
				12,
				60,
				1,
				true,
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
