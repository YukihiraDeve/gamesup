package com.gamesup.api.auth.infrastructure.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityProblemWriter problemWriter;

	public RestAccessDeniedHandler(SecurityProblemWriter problemWriter) {
		this.problemWriter = problemWriter;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException exception) throws IOException, ServletException {
		problemWriter.write(
				response,
				HttpStatus.FORBIDDEN,
				"Forbidden",
				"Your role does not allow access to this resource.",
				request.getRequestURI());
	}
}
