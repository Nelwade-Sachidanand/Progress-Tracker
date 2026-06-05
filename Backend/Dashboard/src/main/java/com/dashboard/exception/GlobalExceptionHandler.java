package com.dashboard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private final ResponseBuilder responseBuilder = new ResponseBuilder();

	@ExceptionHandler(ReadExcelException.class)
	public ResponseEntity<Response> handleDashboardException(ReadExcelException ex) {

		Response response = responseBuilder.createResponse(ex.getErrorCode(),
				StatusCode.ERROR_STATUS_TYPE,ex.getErrorMessage(), null);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Response> handleResourceNotFoundException(ResourceNotFoundException ex) {

		Response response = responseBuilder.createResponse(ex.getErrorCode(),
				StatusCode.ERROR_STATUS_TYPE,ex.getErrorMessage(), null);

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<Response> handleValidationException(ValidationException ex) {

		Response response = responseBuilder.createResponse(ex.getErrorCode(), 
				StatusCode.ERROR_STATUS_TYPE,ex.getErrorMessage(), null);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Response> handleGenericException(Exception ex) {

		Response response = responseBuilder.createResponse(StatusCode.ERROR, "Something went wrong: " + ex.getMessage(),
				StatusCode.ERROR_STATUS_TYPE, null);

		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
