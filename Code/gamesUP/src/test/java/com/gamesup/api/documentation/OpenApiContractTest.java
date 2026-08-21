package com.gamesup.api.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

	private static final Path VERSIONED_CONTRACT = Path.of("docs/openapi.yaml");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void publishedContractMatchesApplicationAndContainsCriticalRoutes() throws Exception {
		String generatedContract = mockMvc.perform(get("/v3/api-docs.yaml"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);

		String output = System.getProperty("gamesup.openapi.output");
		if (output != null && !output.isBlank()) {
			Path outputPath = Path.of(output);
			Files.createDirectories(outputPath.getParent());
			Files.writeString(outputPath, generatedContract);
		}

		assertThat(generatedContract)
				.contains(
						"/api/v1/auth/register:",
						"/api/v1/auth/login:",
						"/api/v1/games:",
						"/api/v1/games/{id}:",
						"/api/v1/users/me:",
						"/api/v1/users/me/wishlist:",
						"/api/v1/games/{gameId}/reviews:",
						"/api/v1/reviews/{reviewId}:",
						"/api/v1/orders:",
						"/api/v1/recommendations:",
						"/api/v1/admin/catalog/games:",
						"/api/v1/admin/recommendations/train:",
						"/api/v1/admin/users:",
						"/api/v1/admin/reviews/{reviewId}/visibility:",
						"/api/v1/admin/orders:",
						"/api/v1/admin/inventory/{gameId}:",
						"bearerAuth:")
				.doesNotContain("passwordHash", "hibernateLazyInitializer", "handler:");

		String generatedJson = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		JsonNode openApi = objectMapper.readTree(generatedJson);
		assertSuccessHasContent(openApi, "/api/v1/auth/login", "post", "200");
		assertSuccessHasContent(openApi, "/api/v1/games", "get", "200");
		assertSuccessHasContent(openApi, "/api/v1/users/me/wishlist", "get", "200");
		assertSuccessHasContent(openApi, "/api/v1/orders", "post", "201");
		assertSuccessHasContent(openApi, "/api/v1/recommendations", "get", "200");
		assertSuccessHasContent(openApi, "/api/v1/admin/recommendations/train", "post", "200");
		assertSuccessHasContent(openApi, "/api/v1/admin/inventory/{gameId}", "get", "200");

		assertThat(VERSIONED_CONTRACT)
				.as("Le contrat OpenAPI versionné doit être généré avant la vérification complète.")
				.exists();
		assertThat(Files.readString(VERSIONED_CONTRACT)).isEqualTo(generatedContract);
	}

	private static void assertSuccessHasContent(
			JsonNode openApi,
			String path,
			String method,
			String status) {
		assertThat(openApi.path("paths").path(path).path(method).path("responses").path(status).path("content").isObject())
				.as("%s %s doit documenter le corps de sa réponse %s.", method, path, status)
				.isTrue();
	}
}
