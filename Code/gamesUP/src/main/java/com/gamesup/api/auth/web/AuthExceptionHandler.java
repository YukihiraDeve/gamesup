package com.gamesup.api.auth.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gamesup.api.auth.application.DuplicateEmailException;

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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		ProblemDetail problem = problem(
				HttpStatus.BAD_REQUEST,
				"Bad Request",
				"The request contains invalid fields.",
				request);
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		problem.setProperty("errors", errors);
		return problem;
	}

	private static ProblemDetail problem(
			HttpStatus status,
			String title,
			String detail,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		return problem;
	}
}
