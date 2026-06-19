package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;

public interface ActivityUpdateRequestService {
	
	Response getAllRequests();

	Response getPendingRequests();

	Response approveRequest(String requestId);

	Response rejectRequest(String requestId, String reason);

	Response approveAllRequests();

	Response rejectAllRequests(String reason);
	
	//Response rollbackActivity(String auditId);
	

}
