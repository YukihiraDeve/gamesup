package com.gamesup.api.recommendation.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.recommendation.application.RecommendationService;
import com.gamesup.api.recommendation.web.dto.RecommendationResponse;
import com.gamesup.api.recommendation.web.dto.TrainingModelResponse;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RecommendationService recommendationService;

	@Test
	void requiresAuthenticationOnBothEndpoints() throws Exception {
		mockMvc.perform(get("/api/v1/recommendations"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/admin/recommendations/train"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(recommendationService);
	}

	@Test
	void allowsOnlyClientsToRequestTheirOwnRecommendations() throws Exception {
		when(recommendationService.recommend(42L, 10))
				.thenReturn(new RecommendationResponse("knn", List.of()));

		mockMvc.perform(get("/api/v1/recommendations")
				.with(authentication(userAuthentication(42L, Role.CLIENT))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value("knn"))
				.andExpect(jsonPath("$.items").isEmpty());
		verify(recommendationService).recommend(42L, 10);

		mockMvc.perform(get("/api/v1/recommendations")
				.with(authentication(userAuthentication(1L, Role.ADMIN))))
				.andExpect(status().isForbidden());
	}

	@Test
	void allowsOnlyAdministratorsToTrain() throws Exception {
		when(recommendationService.train())
				.thenReturn(new TrainingModelResponse("knn", 2, 3, 4));

		mockMvc.perform(post("/api/v1/admin/recommendations/train")
				.with(authentication(userAuthentication(1L, Role.ADMIN))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.retainedInteractions").value(4));
		verify(recommendationService).train();

		mockMvc.perform(post("/api/v1/admin/recommendations/train")
				.with(authentication(userAuthentication(42L, Role.CLIENT))))
				.andExpect(status().isForbidden());
	}

	private static UsernamePasswordAuthenticationToken userAuthentication(long userId, Role role) {
		GamesUpUserPrincipal principal = new GamesUpUserPrincipal(
				userId,
				"technical-user-" + userId,
				"unused",
				role,
				true);
		return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
	}
}
