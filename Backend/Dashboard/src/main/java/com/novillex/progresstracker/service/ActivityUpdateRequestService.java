package com.novillex.progresstracker.service;

import java.util.List;

import com.novillex.progresstracker.common.Response;

public interface ActivityUpdateRequestService {

	Response getAllRequests();
	
	Response getActivityUpdateRequestById(String requestId);

	Response getPendingRequests();

	Response approveRequest(String requestId);

	Response rejectRequest(String requestId, String reason);

	Response approveSelectedRequests(List<String> requestIds);	
	
	Response rollbackActivity(String auditId);
	
	Response rejectSelectedRequests(List<String> requestIds, String reason);
}
