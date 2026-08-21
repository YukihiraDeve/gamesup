package com.gamesup.api.common.application.exception;

public class InvalidExternalServiceResponseException extends RuntimeException {

	public InvalidExternalServiceResponseException(String message) {
		super(message);
	}

	public InvalidExternalServiceResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}
