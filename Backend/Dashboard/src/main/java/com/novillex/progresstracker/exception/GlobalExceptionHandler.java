package com.novillex.progresstracker.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private final ResponseBuilder responseBuilder = new ResponseBuilder();

	@ExceptionHandler(ReadExcelException.class)
	public ResponseEntity<Response> handleDashboardException(ReadExcelException ex) {

		logger.error("Excel processing error. Code: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());

		Response response = responseBuilder.createResponse(ex.getErrorCode(), StatusCode.ERROR_STATUS_TYPE,
				ex.getErrorMessage(), null);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Response> handleResourceNotFoundException(ResourceNotFoundException ex) {

		logger.warn("Resource not found. Code: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());

		Response response = responseBuilder.createResponse(ex.getErrorCode(), StatusCode.ERROR_STATUS_TYPE,
				ex.getErrorMessage(), ex.getResourceName());

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<Response> handleValidationException(ValidationException ex) {

		logger.warn("Validation failed. Code: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());

		Response response = responseBuilder.createResponse(ex.getErrorCode(), StatusCode.ERROR_STATUS_TYPE,
				ex.getErrorMessage(), null);

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(DatabaseException.class)
	public Response handleDatabaseException(DatabaseException ex) {

		logger.error("Database error. Code: {}, Message: {}", ex.getErrorCode(), ex.getErrorMessage());

		return responseBuilder.createResponse(ex.getErrorCode(), StatusCode.ERROR_STATUS_TYPE, ex.getErrorMessage(),
				null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Response> handleGenericException(Exception ex) {

		logger.error("Unexpected application error", ex);

		Response response = responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE,
				"Something went wrong: " + ex.getMessage(), null);

		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}