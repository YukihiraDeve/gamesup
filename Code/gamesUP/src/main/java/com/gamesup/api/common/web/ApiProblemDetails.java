package com.gamesup.api.common.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ApiProblemDetails {

	private ApiProblemDetails() {
	}

	public static ProblemDetail create(
			HttpStatus status,
			String title,
			String detail,
			String requestUri) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("urn:gamesup:problem:" + status.value()));
		problem.setTitle(title);
		problem.setInstance(URI.create(requestUri));
		return problem;
	}
}
