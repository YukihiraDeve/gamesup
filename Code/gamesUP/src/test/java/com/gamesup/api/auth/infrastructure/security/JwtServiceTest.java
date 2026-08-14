package com.gamesup.api.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.gamesup.api.auth.application.GamesUpUserPrincipal;
import com.gamesup.api.auth.domain.Role;
import com.gamesup.api.config.infrastructure.properties.JwtProperties;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

	private static final Instant FIXED_TIME = Instant.parse("2026-08-14T12:00:00Z");
	private static final String SECRET = "test-only-signing-key-with-at-least-32-bytes";

	@Mock
	private UserDetails userDetailsWithoutAuthority;

	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService(
				new JwtProperties("gamesup-test", Duration.ofMinutes(15), SECRET),
				Clock.fixed(FIXED_TIME, ZoneOffset.UTC));
	}

	@Test
	void generatesDeterministicClaimsAndValidatesEnabledPrincipal() {
		GamesUpUserPrincipal principal = principal("client@example.com", true);

		String token = jwtService.generateToken(principal);
		var claims = Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
				.requireIssuer("gamesup-test")
				.clock(() -> Date.from(FIXED_TIME))
				.build()
				.parseSignedClaims(token)
				.getPayload();

		assertThat(claims.getIssuedAt().toInstant()).isEqualTo(FIXED_TIME);
		assertThat(claims.getExpiration().toInstant()).isEqualTo(FIXED_TIME.plusSeconds(900));
		assertThat(claims.get("role", String.class)).isEqualTo("CLIENT");
		assertThat(jwtService.extractUsername(token)).isEqualTo("client@example.com");
		assertThat(jwtService.isTokenValid(token, principal)).isTrue();
		assertThat(jwtService.isTokenValid(token, principal("other@example.com", true))).isFalse();
		assertThat(jwtService.isTokenValid(token, principal("client@example.com", false))).isFalse();
	}

	@Test
	void refusesToGenerateATokenWithoutAuthority() {
		when(userDetailsWithoutAuthority.getAuthorities()).thenReturn(List.of());

		assertThatThrownBy(() -> jwtService.generateToken(userDetailsWithoutAuthority))
				.isInstanceOf(java.util.NoSuchElementException.class);
	}

	private static GamesUpUserPrincipal principal(String email, boolean enabled) {
		return new GamesUpUserPrincipal(
				1L,
				email,
				"encoded-password",
				Role.CLIENT,
				enabled);
	}
}
