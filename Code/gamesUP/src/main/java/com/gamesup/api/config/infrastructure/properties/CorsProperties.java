package com.gamesup.api.config.infrastructure.properties;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("gamesup.cors")
public record CorsProperties(
		@NotEmpty List<@NotBlank String> allowedOrigins) {
}
