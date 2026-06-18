package com.dashboard.service;

import com.dashboard.common.Response;

public interface ActivityUpdateRequestService {

	    Response getPendingRequests();

	    Response approveRequest(String requestId);

	    Response rejectRequest(
	            String requestId,
	            String reason);
	

}
