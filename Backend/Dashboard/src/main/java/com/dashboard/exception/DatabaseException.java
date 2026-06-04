package com.dashboard.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseException extends RuntimeException{
	
	 private  String errorCode;
	    private  String errorMessage;

}
