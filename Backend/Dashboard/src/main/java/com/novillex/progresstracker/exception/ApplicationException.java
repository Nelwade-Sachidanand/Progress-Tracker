package com.novillex.progresstracker.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationException extends RuntimeException {

	private String errorCode;
	private String errorMessage;
    
}
