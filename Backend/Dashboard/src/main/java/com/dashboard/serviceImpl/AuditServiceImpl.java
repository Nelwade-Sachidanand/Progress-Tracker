package com.dashboard.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.AuditLog;
import com.dashboard.repository.AuditLogRepository;
import com.dashboard.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditServiceImpl implements AuditService {

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private ResponseBuilder responseBuilder;

	@Override
	public void saveAuditLog(String actionType, String entityType, String entityName, String projectName,
			Object oldObject, Object newObject, String modifiedBy) {

		try{

			AuditLog auditLog = new AuditLog();
			auditLog.setActionType(actionType);
			auditLog.setEntityType(entityType);
			auditLog.setEntityName(entityName);
			auditLog.setProjectName(projectName);
			auditLog.setModifiedBy(modifiedBy);
			auditLog.setModifiedDate(LocalDateTime.now());
			auditLog.setOldData(oldObject == null ? null : objectMapper.writeValueAsString(oldObject));
			auditLog.setNewData(newObject == null ? null : objectMapper.writeValueAsString(newObject));
			auditLogRepository.save(auditLog);

		}catch (Exception e) {
			throw new RuntimeException("Error while saving audit log", e);
		}
	}

	@Override
	public Response getAuditLogs() {
		List<AuditLog> result = auditLogRepository.findAll();
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Audits Fetched Successfully", result);
	}
}