package com.novillex.progresstracker.exception;

import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {
	 private static final long serialVersionUID = 1L;
	 
	private String errorCode;
	private String errorMessage;
	
	
	public ApplicationException(String errorCode, String errorMessage) {
		super();
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
    
}
