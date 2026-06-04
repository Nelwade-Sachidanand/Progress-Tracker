package com.dashboard.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
	public List<AuditLog> getAuditLogs() {
		return auditLogRepository.findAll();
	}
}