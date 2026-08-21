package com.gamesup.api.config.infrastructure.properties;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("gamesup.recommendation")
public record RecommendationProperties(
		@NotNull URI baseUrl,
		@NotBlank @Size(min = 16) String serviceKey,
		@NotNull Duration connectTimeout,
		@NotNull Duration readTimeout) {

	@AssertTrue(message = "gamesup.recommendation.base-url must be an absolute HTTP(S) URL")
	public boolean isBaseUrlValid() {
		return baseUrl != null
				&& baseUrl.isAbsolute()
				&& ("http".equalsIgnoreCase(baseUrl.getScheme()) || "https".equalsIgnoreCase(baseUrl.getScheme()));
	}

	@AssertTrue(message = "gamesup.recommendation timeouts must be strictly positive")
	public boolean areTimeoutsValid() {
		return isStrictlyPositive(connectTimeout) && isStrictlyPositive(readTimeout);
	}

	private static boolean isStrictlyPositive(Duration duration) {
		return duration != null && !duration.isNegative() && !duration.isZero();
	}
}
