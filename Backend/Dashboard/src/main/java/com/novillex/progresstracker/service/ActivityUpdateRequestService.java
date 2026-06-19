package com.novillex.progresstracker.service;

import java.util.List;

import com.novillex.progresstracker.common.Response;

public interface ActivityUpdateRequestService {

	Response getAllRequests();

	Response getPendingRequests();

	Response approveRequest(String requestId);

	Response rejectRequest(String requestId, String reason);

	Response approveSelectedRequests(List<String> requestIds);

	Response rejectSelectedRequests(List<String> requestIds, String reason);
}
