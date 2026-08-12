package com.gamesup.api.common.application.exception;

public class BusinessRuleViolationException extends RuntimeException {

	public BusinessRuleViolationException(String message) {
		super(message);
	}
}
