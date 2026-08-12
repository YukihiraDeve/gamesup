package com.gamesup.api.common.web;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.gamesup.api.common.application.exception.BusinessRuleViolationException;
import com.gamesup.api.common.application.exception.ConflictException;
import com.gamesup.api.common.application.exception.ExternalServiceException;
import com.gamesup.api.common.application.exception.ForbiddenOperationException;
import com.gamesup.api.common.application.exception.ResourceNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request);
	}

	@ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
	ProblemDetail handleForbidden(RuntimeException exception, HttpServletRequest request) {
		return problem(HttpStatus.FORBIDDEN, "Forbidden operation", exception.getMessage(), request);
	}

	@ExceptionHandler(BusinessRuleViolationException.class)
	ProblemDetail handleBusinessRule(BusinessRuleViolationException exception, HttpServletRequest request) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", exception.getMessage(), request);
	}

	@ExceptionHandler(ExternalServiceException.class)
	ProblemDetail handleExternalService(ExternalServiceException exception, HttpServletRequest request) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "External service unavailable", exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleMethodArgumentValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		ProblemDetail problem = invalidRequest("The request contains invalid fields.", request);
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		exception.getBindingResult().getGlobalErrors()
				.forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(BindException.class)
	ProblemDetail handleBinding(BindException exception, HttpServletRequest request) {
		ProblemDetail problem = invalidRequest("The request parameters are invalid.", request);
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintValidation(
			ConstraintViolationException exception,
			HttpServletRequest request) {
		ProblemDetail problem = invalidRequest("The request parameters violate validation constraints.", request);
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getConstraintViolations().forEach(violation -> errors.putIfAbsent(
				violation.getPropertyPath().toString(), violation.getMessage()));
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleMethodValidation(
			HandlerMethodValidationException exception,
			HttpServletRequest request) {
		ProblemDetail problem = invalidRequest("The request parameters violate validation constraints.", request);
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getAllValidationResults().forEach(result -> {
			String parameter = result.getMethodParameter().getParameterName();
			result.getResolvableErrors().forEach(error -> errors.putIfAbsent(
					parameter == null ? "parameter" : parameter,
					error.getDefaultMessage()));
		});
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception, HttpServletRequest request) {
		return invalidRequest("The request body is missing or contains invalid JSON.", request);
	}

	@ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
	ProblemDetail handleInvalidParameter(Exception exception, HttpServletRequest request) {
		return invalidRequest("A request parameter is missing or has an invalid value.", request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
		return problem(
				HttpStatus.CONFLICT,
				"Data conflict",
				"The operation conflicts with an existing resource or database constraint.",
				request);
	}

	private static ProblemDetail invalidRequest(String detail, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail, request);
	}

	private static ProblemDetail problem(
			HttpStatus status,
			String title,
			String detail,
			HttpServletRequest request) {
		return ApiProblemDetails.create(status, title, detail, request.getRequestURI());
	}
}
