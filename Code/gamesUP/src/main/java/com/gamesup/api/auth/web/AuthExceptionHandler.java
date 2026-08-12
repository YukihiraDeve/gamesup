package com.gamesup.api.auth.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gamesup.api.auth.application.DuplicateEmailException;
import com.gamesup.api.common.web.ApiProblemDetails;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

	@ExceptionHandler(DuplicateEmailException.class)
	ProblemDetail handleDuplicateEmail(DuplicateEmailException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request);
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthenticationFailure(AuthenticationException exception, HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password.", request);
	}

	private static ProblemDetail problem(
			HttpStatus status,
			String title,
			String detail,
			HttpServletRequest request) {
		return ApiProblemDetails.create(status, title, detail, request.getRequestURI());
	}
}
