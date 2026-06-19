package com.novillex.progresstracker.service;

import java.util.List;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.entity.AuditLog;

public interface AuditService {
	
	void saveAuditLog(String actionType, String entityType, String entityName, String projectName, Object oldObject,
			Object newObject, String modifiedBy);
	
	Response getAuditLogs();
	
}
