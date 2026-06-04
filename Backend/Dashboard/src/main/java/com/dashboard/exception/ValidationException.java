package com.dashboard.exception;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {

	private final String errorCode;
	private final String errorMessage;

	public ValidationException(String errorCode, String errorMessage) {

		super(errorMessage);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
}