package com.novillex.progresstracker.exception;

import lombok.Getter;

@Getter
public class DatabaseException extends RuntimeException {
	 private static final long serialVersionUID = 1L;
	 
	 
	private String errorCode;
	private String errorMessage;
	public DatabaseException(String errorCode, String errorMessage) {
		super();
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
	
	

}
