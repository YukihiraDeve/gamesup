package com.gamesup.api.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.gamesup.api.config.infrastructure.properties.JwtProperties;

@Service
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey signingKey;
	private final Clock clock;

	@Autowired
	public JwtService(JwtProperties properties) {
		this(properties, Clock.systemUTC());
	}

	JwtService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.clock = clock;
	}

	public String generateToken(UserDetails userDetails) {
		Instant issuedAt = Instant.now(clock);
		Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
		String role = userDetails.getAuthorities().stream()
				.findFirst()
				.map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
				.orElseThrow();

		return Jwts.builder()
				.issuer(properties.issuer())
				.subject(userDetails.getUsername())
				.claim("role", role)
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(signingKey)
				.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		return parseClaims(token).getSubject().equalsIgnoreCase(userDetails.getUsername())
				&& userDetails.isEnabled();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.requireIssuer(properties.issuer())
				.clock(() -> Date.from(Instant.now(clock)))
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
