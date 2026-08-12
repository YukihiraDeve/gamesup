package com.gamesup.api.auth.infrastructure.security;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesup.api.common.web.ApiProblemDetails;

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
		ProblemDetail problem = ApiProblemDetails.create(status, title, detail, requestUri);

		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
