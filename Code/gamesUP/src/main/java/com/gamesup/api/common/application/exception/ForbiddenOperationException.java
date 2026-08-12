package com.gamesup.api.common.application.exception;

public class ForbiddenOperationException extends RuntimeException {

	public ForbiddenOperationException(String message) {
		super(message);
	}
}
