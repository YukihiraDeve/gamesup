package com.gamesup.api.auth.infrastructure.security;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SecurityProblemWriter {

	private final ObjectMapper objectMapper;

	public SecurityProblemWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void write(
			HttpServletResponse response,
			HttpStatus status,
			String title,
			String detail,
			String requestUri) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setInstance(URI.create(requestUri));

		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
