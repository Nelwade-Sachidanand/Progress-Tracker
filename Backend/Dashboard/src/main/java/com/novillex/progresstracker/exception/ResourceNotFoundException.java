package com.novillex.progresstracker.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
	 private static final long serialVersionUID = 1L;

    private  String errorCode;
    private  String errorMessage;
    private  String resourceName;

    public ResourceNotFoundException(String errorCode, String errorMessage, String resourceName) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.resourceName=resourceName;
    }
}