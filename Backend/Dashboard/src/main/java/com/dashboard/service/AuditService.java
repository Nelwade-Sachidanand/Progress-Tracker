package com.dashboard.service;

import java.util.List;

import com.dashboard.entity.AuditLog;

public interface AuditService {
	
	void saveAuditLog(String actionType, String entityType, String entityName, String projectName, Object oldObject,
			Object newObject, String modifiedBy);
	
	List<AuditLog> getAuditLogs();
}
