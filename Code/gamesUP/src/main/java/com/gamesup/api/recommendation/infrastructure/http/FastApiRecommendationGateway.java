package com.gamesup.api.recommendation.infrastructure.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.gamesup.api.common.application.exception.BusinessRuleViolationException;
import com.gamesup.api.common.application.exception.ExternalServiceException;
import com.gamesup.api.common.application.exception.InvalidExternalServiceResponseException;
import com.gamesup.api.config.infrastructure.properties.RecommendationProperties;
import com.gamesup.api.recommendation.application.port.RecommendationGateway;

@Component
public class FastApiRecommendationGateway implements RecommendationGateway {

	private static final String SERVICE_KEY_HEADER = "X-Service-Key";
	private static final String UNAVAILABLE_MESSAGE = "Recommendation service is unavailable.";
	private static final String INVALID_RESPONSE_MESSAGE = "Recommendation service returned an invalid response.";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public FastApiRecommendationGateway(
			RecommendationProperties properties,
			ObjectMapper objectMapper,
			RestClient.Builder restClientBuilder) {
		this.objectMapper = objectMapper;
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());
		this.restClient = restClientBuilder.clone()
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.defaultHeader(SERVICE_KEY_HEADER, properties.serviceKey())
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	@Override
	public TrainingResult train(List<Interaction> interactions) {
		TrainingRequest request = new TrainingRequest(interactions.stream()
				.map(interaction -> new TrainingInteractionRequest(
						interaction.userId(),
						interaction.gameId(),
						interaction.rating()))
				.toList());
		try {
			TrainingResponse response = restClient.post()
					.uri("/model/train")
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.exchange((httpRequest, httpResponse) -> {
						if (httpResponse.getStatusCode().value() == 422) {
							throw new BusinessRuleViolationException(
									"Recommendation training data is insufficient.");
						}
						requireSuccessfulResponse(httpResponse.getStatusCode().is2xxSuccessful());
						return readResponse(httpResponse.getBody(), TrainingResponse.class);
					});
		validate(response);
		return new TrainingResult(
				response.version(),
				response.users(),
				response.games(),
				response.retainedInteractions());
		} catch (BusinessRuleViolationException | ExternalServiceException
				| InvalidExternalServiceResponseException exception) {
			throw exception;
		} catch (ResourceAccessException exception) {
			throw new ExternalServiceException(UNAVAILABLE_MESSAGE, exception);
		} catch (RestClientException exception) {
			throw new ExternalServiceException(UNAVAILABLE_MESSAGE, exception);
		}
	}

	@Override
	public RecommendationResult recommend(long userId, List<Interaction> history, int limit) {
		RecommendationRequest request = new RecommendationRequest(
				userId,
				history.stream()
						.map(interaction -> new RecommendationHistoryRequest(
								interaction.gameId(),
								interaction.rating()))
						.toList(),
				limit);
		try {
			RecommendationResponse response = restClient.post()
					.uri("/recommendations")
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.exchange((httpRequest, httpResponse) -> {
						requireSuccessfulResponse(httpResponse.getStatusCode().is2xxSuccessful());
						return readResponse(httpResponse.getBody(), RecommendationResponse.class);
					});
		validate(response);
		return new RecommendationResult(
				response.version(),
				response.items().stream()
						.map(item -> new Candidate(item.gameId(), item.score()))
						.toList());
		} catch (ExternalServiceException | InvalidExternalServiceResponseException exception) {
			throw exception;
		} catch (ResourceAccessException exception) {
			throw new ExternalServiceException(UNAVAILABLE_MESSAGE, exception);
		} catch (RestClientException exception) {
			throw new ExternalServiceException(UNAVAILABLE_MESSAGE, exception);
		}
	}

	private <T> T readResponse(InputStream body, Class<T> responseType) {
		try {
			return objectMapper.readerFor(responseType)
					.with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
					.readValue(body);
		} catch (IOException | RuntimeException exception) {
			throw new InvalidExternalServiceResponseException(INVALID_RESPONSE_MESSAGE, exception);
		}
	}

	private static void requireSuccessfulResponse(boolean successful) {
		if (!successful) {
			throw new ExternalServiceException(UNAVAILABLE_MESSAGE);
		}
	}

	private static void validate(TrainingResponse response) {
		if (response == null
				|| response.version() == null
				|| response.version().isBlank()
				|| response.users() < 2
				|| response.games() < 2
				|| response.retainedInteractions() < 2) {
			throw new InvalidExternalServiceResponseException(INVALID_RESPONSE_MESSAGE);
		}
	}

	private static void validate(RecommendationResponse response) {
		if (response == null
				|| response.version() == null
				|| response.version().isBlank()
				|| response.items() == null) {
			throw new InvalidExternalServiceResponseException(INVALID_RESPONSE_MESSAGE);
		}
		Set<Long> ids = new HashSet<>();
		boolean invalidItem = response.items().stream().anyMatch(item -> item == null
				|| item.gameId() <= 0
				|| !Double.isFinite(item.score())
				|| item.score() < 0.0
				|| item.score() > 1.0
				|| !ids.add(item.gameId()));
		if (invalidItem) {
			throw new InvalidExternalServiceResponseException(INVALID_RESPONSE_MESSAGE);
		}
	}

	private record TrainingRequest(List<TrainingInteractionRequest> interactions) {
	}

	private record TrainingInteractionRequest(
			@JsonProperty("user_id") long userId,
			@JsonProperty("game_id") long gameId,
			double rating) {
	}

	private record TrainingResponse(
			String version,
			int users,
			int games,
			@JsonProperty("retained_interactions") int retainedInteractions) {
	}

	private record RecommendationRequest(
			@JsonProperty("user_id") long userId,
			List<RecommendationHistoryRequest> history,
			int limit) {
	}

	private record RecommendationHistoryRequest(
			@JsonProperty("game_id") long gameId,
			double rating) {
	}

	private record RecommendationResponse(String version, List<RecommendationItemResponse> items) {
	}

	private record RecommendationItemResponse(
			@JsonProperty("game_id") long gameId,
			double score) {
	}
}
