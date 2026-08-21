package com.gamesup.api.recommendation.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.gamesup.api.common.application.exception.BusinessRuleViolationException;
import com.gamesup.api.common.application.exception.ExternalServiceException;
import com.gamesup.api.common.application.exception.InvalidExternalServiceResponseException;
import com.gamesup.api.config.infrastructure.properties.RecommendationProperties;
import com.gamesup.api.recommendation.application.port.RecommendationGateway.Interaction;

class FastApiRecommendationGatewayTest {

	private static final String SERVICE_KEY = "test-fastapi-key-22";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockWebServer server;
	private FastApiRecommendationGateway gateway;

	@BeforeEach
	void setUp() throws Exception {
		server = new MockWebServer();
		server.start();
		gateway = gateway(Duration.ofMillis(250));
	}

	@AfterEach
	void tearDown() throws Exception {
		server.shutdown();
	}

	@Test
	void sendsOnlyTechnicalTrainingDataAndReadsTheExactSuccessContract() throws Exception {
		server.enqueue(jsonResponse(200, """
				{
				  "version": "knn-20260821",
				  "users": 2,
				  "games": 3,
				  "retained_interactions": 4
				}
				"""));

		var result = gateway.train(List.of(
				new Interaction(11L, 101L, 5.0),
				new Interaction(12L, 102L, 3.0)));

		assertThat(result.version()).isEqualTo("knn-20260821");
		assertThat(result.retainedInteractions()).isEqualTo(4);
		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath()).isEqualTo("/model/train");
		assertThat(request.getHeader("X-Service-Key")).isEqualTo(SERVICE_KEY);
		JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
		assertThat(body.path("interactions").get(0).fieldNames())
				.toIterable()
				.containsExactlyInAnyOrder("user_id", "game_id", "rating");
		assertThat(body.toString())
				.doesNotContain("email", "firstName", "lastName", "password", "comment");
	}

	@Test
	void sendsTheCurrentTechnicalHistoryAndReadsOrderedCandidates() throws Exception {
		server.enqueue(jsonResponse(200, """
				{
				  "version": "knn-20260821",
				  "items": [
				    {"game_id": 31, "score": 0.9},
				    {"game_id": 18, "score": 0.7}
				  ]
				}
				"""));

		var result = gateway.recommend(7L, List.of(new Interaction(7L, 4L, 3.0)), 2);

		assertThat(result.items()).extracting(item -> item.gameId()).containsExactly(31L, 18L);
		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath()).isEqualTo("/recommendations");
		JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
		assertThat(body.fieldNames()).toIterable()
				.containsExactlyInAnyOrder("user_id", "history", "limit");
		assertThat(body.path("history").get(0).fieldNames()).toIterable()
				.containsExactlyInAnyOrder("game_id", "rating");
	}

	@Test
	void mapsReadTimeoutToServiceUnavailable() {
		gateway = gateway(Duration.ofMillis(30));
		server.enqueue(jsonResponse(200, "{\"version\":\"late\",\"items\":[]}")
				.setHeadersDelay(250, TimeUnit.MILLISECONDS));

		assertThatThrownBy(() -> gateway.recommend(7L, List.of(), 10))
				.isInstanceOf(ExternalServiceException.class)
				.hasMessage("Recommendation service is unavailable.");
	}

	@Test
	void mapsTrainingRejectionToUnprocessableBusinessRule() {
		server.enqueue(jsonResponse(422, "{\"detail\":\"training dataset is insufficient\"}"));

		assertThatThrownBy(() -> gateway.train(List.of(new Interaction(1L, 1L, 3.0))))
				.isInstanceOf(BusinessRuleViolationException.class)
				.hasMessage("Recommendation training data is insufficient.");
	}

	@Test
	void mapsFastApiUnavailabilityToServiceUnavailable() {
		server.enqueue(jsonResponse(503, "{\"detail\":\"model is not available\"}"));

		assertThatThrownBy(() -> gateway.recommend(7L, List.of(), 10))
				.isInstanceOf(ExternalServiceException.class)
				.hasMessage("Recommendation service is unavailable.");
	}

	@Test
	void mapsMalformedOrStructurallyInvalidJsonToBadGateway() {
		server.enqueue(jsonResponse(200, "{not-json"));
		assertThatThrownBy(() -> gateway.recommend(7L, List.of(), 10))
				.isInstanceOf(InvalidExternalServiceResponseException.class);

		server.enqueue(jsonResponse(200, """
				{"version":"knn","items":[{"game_id":4,"score":1.5}]}
				"""));
		assertThatThrownBy(() -> gateway.recommend(7L, List.of(), 10))
				.isInstanceOf(InvalidExternalServiceResponseException.class);
	}

	private FastApiRecommendationGateway gateway(Duration readTimeout) {
		RecommendationProperties properties = new RecommendationProperties(
				server.url("/").uri(),
				SERVICE_KEY,
				Duration.ofMillis(250),
				readTimeout);
		return new FastApiRecommendationGateway(properties, objectMapper, RestClient.builder());
	}

	private static MockResponse jsonResponse(int status, String body) {
		return new MockResponse()
				.setResponseCode(status)
				.setHeader("Content-Type", "application/json")
				.setBody(body);
	}
}
