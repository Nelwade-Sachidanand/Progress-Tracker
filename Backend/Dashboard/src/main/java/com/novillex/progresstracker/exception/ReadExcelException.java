package com.novillex.progresstracker.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadExcelException extends RuntimeException {
	private String errorCode;
    private String errorMessage;
}
