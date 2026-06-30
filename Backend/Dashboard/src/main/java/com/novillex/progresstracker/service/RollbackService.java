package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;

public interface RollbackService {

	Response rollbackRequest(String requestId, String password, String reason);
}
