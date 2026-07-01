package com.novillex.progresstracker.service;


import com.novillex.progresstracker.common.Response;

public interface AuditService {
	
	void saveAuditLog(String actionType, String entityType, String entityName, String projectName, Object oldObject,
			Object newObject, String modifiedBy);
	
	Response getAuditLogs();
	
}
