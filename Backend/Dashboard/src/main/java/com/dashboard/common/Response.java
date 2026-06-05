package com.dashboard.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
	
	private String statusCode;
	private String statusType;
	private String statusDesc;
	private Object details;
}
