package com.dashboard.common;

import org.springframework.stereotype.Component;

@Component
public class ResponseBuilder {
	
	public Response createResponse(String code, String type, String desc, Object details) {
        Response response = new Response();
        response.setStatusCode(code);
        response.setStatusDesc(desc);
        response.setStatusType(type);
        response.setDetails(details);
        return response;
    }
}

