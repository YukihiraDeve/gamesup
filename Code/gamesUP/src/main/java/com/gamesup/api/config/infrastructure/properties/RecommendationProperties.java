package com.gamesup.api.config.infrastructure.properties;

import java.net.URI;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("gamesup.recommendation")
public record RecommendationProperties(@NotNull URI baseUrl) {

	@AssertTrue(message = "gamesup.recommendation.base-url must be an absolute HTTP(S) URL")
	public boolean isBaseUrlValid() {
		return baseUrl != null
				&& baseUrl.isAbsolute()
				&& ("http".equalsIgnoreCase(baseUrl.getScheme()) || "https".equalsIgnoreCase(baseUrl.getScheme()));
	}
}
