package com.gamesup.api.config.infrastructure.properties;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("gamesup.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@NotNull Duration accessTokenTtl,
		@NotBlank @Size(min = 32) String secret) {

	@AssertTrue(message = "gamesup.jwt.access-token-ttl must be positive")
	public boolean isAccessTokenTtlPositive() {
		return accessTokenTtl != null && !accessTokenTtl.isZero() && !accessTokenTtl.isNegative();
	}
}
