package com.gamesup.api.auth.infrastructure.security;

import org.springframework.security.core.AuthenticationException;

public class InvalidTokenAuthenticationException extends AuthenticationException {

	public InvalidTokenAuthenticationException(Throwable cause) {
		super("Access token is invalid or expired.", cause);
	}
}
