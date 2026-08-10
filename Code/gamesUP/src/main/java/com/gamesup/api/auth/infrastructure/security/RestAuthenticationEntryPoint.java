package com.gamesup.api.auth.infrastructure.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityProblemWriter problemWriter;

	public RestAuthenticationEntryPoint(SecurityProblemWriter problemWriter) {
		this.problemWriter = problemWriter;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String detail = exception instanceof InvalidTokenAuthenticationException
				? exception.getMessage()
				: "Authentication is required to access this resource.";
		problemWriter.write(response, HttpStatus.UNAUTHORIZED, "Unauthorized", detail, request.getRequestURI());
	}
}
