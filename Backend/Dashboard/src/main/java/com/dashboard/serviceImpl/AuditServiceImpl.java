package com.dashboard.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

<<<<<<< HEAD
import com.dashboard.common.ErrorCode;
=======
>>>>>>> c3a1570c35f269824906dd6100edf7c1feb8f519
import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.AuditLog;
import com.dashboard.exception.DatabaseException;
import com.dashboard.exception.ResourceNotFoundException;
import com.dashboard.repository.AuditLogRepository;
import com.dashboard.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditServiceImpl implements AuditService {

	private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private ObjectMapper objectMapper;
<<<<<<< HEAD
	@Autowired
	private ApplicationContext context;
=======
	
	@Autowired
	private ResponseBuilder responseBuilder;
>>>>>>> c3a1570c35f269824906dd6100edf7c1feb8f519

	@Override
	public void saveAuditLog(String actionType, String entityType, String entityName, String projectName,
			Object oldObject, Object newObject, String modifiedBy) {
		logger.info("Saving audit log. Action: {}, Entity: {}, Name: {}, User: {}", actionType, entityType, entityName,
				modifiedBy);

		try {

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
			
			logger.info("Audit log saved successfully. Action: {}, Entity: {}", actionType, entityType);
		} catch (Exception e) {
			logger.error("Failed to save audit log. Action: {}, Entity: {}", actionType, entityType, e);
			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Error while saving audit log");
		}
	}

	@Override
	public Response getAuditLogs() {
<<<<<<< HEAD

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			List<AuditLog> auditLogs = auditLogRepository.findAll();

			if (auditLogs == null || auditLogs.isEmpty()) {
				logger.warn("No audit logs found");
				throw new ResourceNotFoundException(ErrorCode.AUDIT_NOT_FOUND, "No audit logs found", null);
			}
			logger.info("Audit logs fetched successfully. Count: {}", auditLogs.size());
			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Audit logs fetched successfully", auditLogs);

		} catch (ResourceNotFoundException e) {

			throw e;

		} catch (Exception e) {

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Error while fetching audit logs");
		}
=======
		List<AuditLog> result = auditLogRepository.findAll();
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Audits Fetched Successfully", result);
>>>>>>> c3a1570c35f269824906dd6100edf7c1feb8f519
	}
}