package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.ActivityUpdateRequest;
import com.novillex.progresstracker.entity.AuditLog;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.HierarchyReference;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.AuditLogRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.ActivityUpdateRequestService;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.util.HierarchyReferenceUtil;
import com.novillex.progresstracker.util.UserContextUtil;

@Service
public class ActivityUpdateRequestServiceImpl implements ActivityUpdateRequestService {

	@Autowired
	private ActivityUpdateRequestRepository requestRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private AuditService auditService;

	@Autowired
	AuditLogRepository auditLogRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private ObjectMapper objectMapper;

	private static final Logger logger = LoggerFactory.getLogger(ActivityUpdateRequestServiceImpl.class);

	@Override
	public Response getPendingRequests() {
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		logger.info("Fetching pending activity update requests.");

		try {

			List<ActivityUpdateRequest> requests = requestRepository.findByStatus("PENDING");

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Pending requests fetched successfully.", requests);

		} catch (Exception ex) {

			logger.error("Failed to fetch pending activity update requests.", ex);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to fetch pending activity update requests.");
		}
	}

	@Override
	public Response approveRequest(String requestId) {

		logger.info("Activity update approval initiated. RequestId={}, ApprovedBy={}", requestId,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ActivityUpdateRequest request = requestRepository.findById(requestId).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Request not found", requestId));

		Project project = projectRepository.findById(request.getProjectId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
						request.getProjectId()));

		HierarchyReference ref = HierarchyReferenceUtil.findHierarchy(project, request.getActivityId());

		// ==========================
		// Update hierarchy names
		// ==========================

		ref.getPhase().setPhaseName(request.getNewPhaseName());

		ref.getMilestone().setMilestoneName(request.getNewMilestoneName());

		ref.getTask().setTaskName(request.getNewTaskName());

		ref.getSubTask().setSubTaskName(request.getNewSubTaskName());

		// ==========================
		// Update Activity
		// ==========================

		BeanUtils.copyProperties(request.getNewActivity(), ref.getActivity());

		projectRepository.save(project);

		// ==========================
		// Update Request
		// ==========================

		request.setStatus("APPROVED");

		request.setApprovedBy(UserContextUtil.getCurrentUser());

		request.setApprovedByUserId(UserContextUtil.getCurrentUserId());

		request.setApprovedAt(LocalDateTime.now());

		requestRepository.save(request);

		// ==========================
		// Notification
		// ==========================

		notificationService.createNotification("Activity Update Approved",
				"Your update request for activity '" + request.getNewActivityName() + "' has been approved.",
				"ACTIVITY_APPROVED", request.getId(), "/tasks", request.getRequestedByUserId());

		// ==========================
		// Audit
		// ==========================

		auditService.saveAuditLog(AuditAction.APPROVE_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
				request.getNewActivityName(), project.getProjectName(), request.getOldActivity(),
				request.getNewActivity(), UserContextUtil.getCurrentUser());

		logger.info("Activity update approved successfully. RequestId={}, Activity={}", requestId,
				request.getNewActivityName());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Request approved successfully", request);
	}

	@Override
	public Response approveSelectedRequests(List<String> requestIds) {

		logger.info("Bulk activity approval initiated. TotalRequestIds={}, ApprovedBy={}", requestIds.size(),
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ActivityUpdateRequest> requests = requestRepository.findAllById(requestIds);

		if (requests.isEmpty()) {

			throw new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "No requests found", null);
		}

		String approvedBy = UserContextUtil.getCurrentUser();

		List<Activity> oldActivities = new ArrayList<>();

		List<Activity> newActivities = new ArrayList<>();

		for (ActivityUpdateRequest request : requests) {

			if (!"PENDING".equals(request.getStatus())) {
				continue;
			}

			oldActivities.add(request.getOldActivity());

			newActivities.add(request.getNewActivity());

			Project project = projectRepository.findById(request.getProjectId())
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
							request.getProjectId()));

			HierarchyReference ref = HierarchyReferenceUtil.findHierarchy(project, request.getActivityId());

			ref.getPhase().setPhaseName(request.getNewPhaseName());

			ref.getMilestone().setMilestoneName(request.getNewMilestoneName());

			ref.getTask().setTaskName(request.getNewTaskName());

			ref.getSubTask().setSubTaskName(request.getNewSubTaskName());

			BeanUtils.copyProperties(request.getNewActivity(), ref.getActivity());

			if ("Implementation User".equalsIgnoreCase(request.getRequestedByRole())) {

			    activity.setLocked(true);
			    activity.setLockedBy(UserContextUtil.getCurrentUser());
			    activity.setLockedAt(LocalDateTime.now());

			} else {

			    activity.setLocked(false);
			    activity.setLockedBy(null);
			    activity.setLockedAt(null);
			}
			projectRepository.save(project);

			request.setStatus("APPROVED");

			request.setApprovedBy(UserContextUtil.getCurrentUser());
			request.setApprovedAt(LocalDateTime.now());

			notificationService.createNotification("Activity Update Approved",
					"Your update request for activity '" + request.getNewActivityName() + "' has been approved.",
					"ACTIVITY_APPROVED", request.getId(), "/tasks", request.getRequestedByUserId());

			auditService.saveAuditLog(AuditAction.APPROVE_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
					request.getActivityName(), project.getProjectName(), request.getOldActivity(),
					request.getNewActivity(), UserContextUtil.getCurrentUser(), request.getRequestedByRole());

			logger.info(
					"Activity update approved successfully. RequestId={}, ProjectName={}, ActivityName={}, ApprovedBy={}",
					requestId, project.getProjectName(), request.getActivityName(), UserContextUtil.getCurrentUser());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Request approved successfully", request);

		} catch (ResourceNotFoundException ex) {

			logger.error("Activity approval failed. RequestId={}, Reason={}", requestId, ex.getMessage());

			throw ex;

		} catch (Exception ex) {

			logger.error("Unexpected error while approving activity update. RequestId={}", requestId, ex);

			throw ex;
		}

		requestRepository.saveAll(requests);

		auditService.saveAuditLog(AuditAction.APPROVE_ALL_ACTIVITY_UPDATES, AuditEntity.ACTIVITY,
				"Bulk Approval (" + requests.size() + " Requests)", null, oldActivities, newActivities, approvedBy);

		logger.info("Bulk activity approval completed successfully. RequestedCount={}, ApprovedBy={}",
				requestIds.size(), approvedBy);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				requests.size() + " requests approved successfully", requests.size());
	}

	@Override
	public Response rejectRequest(String requestId, String reason) {

		logger.info("Activity update rejection initiated. RequestId={}, RejectedBy={}", requestId,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ActivityUpdateRequest request = requestRepository.findById(requestId).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Request not found", requestId));

		request.setStatus("REJECTED");

		request.setRejectionReason(reason);

		request.setApprovedBy(UserContextUtil.getCurrentUser());

		request.setApprovedByUserId(UserContextUtil.getCurrentUserId());

		request.setApprovedAt(LocalDateTime.now());

		requestRepository.save(request);

		notificationService.createNotification("Activity Update Rejected",
				"Your update request for activity '" + request.getNewActivityName() + "' has been rejected.",
				"ACTIVITY_REJECTED", request.getId(), "/tasks", request.getRequestedByUserId());

		auditService.saveAuditLog(AuditAction.REJECT_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
				request.getNewActivityName(), request.getProjectName(), request.getOldActivity(),
				request.getNewActivity(), UserContextUtil.getCurrentUser());

			logger.error("Activity rejection failed. RequestId={}, Reason={}", requestId, ex.getMessage());

			throw ex;

		} catch (Exception ex) {

			logger.error("Unexpected error while rejecting activity update. RequestId={}", requestId, ex);

			throw ex;
		}
	}

	@Override
	public Response approveSelectedRequests(List<String> requestIds) {
		logger.info("Bulk activity approval initiated. TotalRequestIds={}, ApprovedBy={}", requestIds.size(),
				UserContextUtil.getCurrentUser());
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ActivityUpdateRequest> requests = requestRepository.findAllById(requestIds);

		if (requests.isEmpty()) {
			logger.warn("Bulk activity approval failed. No requests found. RequestIds={}", requestIds);
			throw new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "No requests found", null);
		}

		String approvedBy = UserContextUtil.getCurrentUser();

		List<Activity> oldActivities = new ArrayList<>();

		List<Activity> newActivities = new ArrayList<>();

		for (ActivityUpdateRequest request : requests) {

			if (!"PENDING".equals(request.getStatus())) {
				continue;
			}

			oldActivities.add(request.getOldActivity());

			newActivities.add(request.getNewActivity());

			Project project = projectRepository.findById(request.getProjectId())
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
							request.getProjectId()));

			Activity activity = findActivity(project, request);

			BeanUtils.copyProperties(request.getNewActivity(), activity);

			if ("Implementation User".equalsIgnoreCase(request.getRequestedByRole())) {

			    activity.setLocked(true);
			    activity.setLockedBy(UserContextUtil.getCurrentUser());
			    activity.setLockedAt(LocalDateTime.now());

			} else {

			    activity.setLocked(false);
			    activity.setLockedBy(null);
			    activity.setLockedAt(null);
			}

			projectRepository.save(project);

			request.setStatus("APPROVED");
			request.setApprovedBy(approvedBy);
			request.setApprovedAt(LocalDateTime.now());
			auditService.saveAuditLog(AuditAction.APPROVE_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
					request.getActivityName(), project.getProjectName(), request.getOldActivity(),
					request.getNewActivity(), approvedBy, request.getRequestedByRole());
		}

		requestRepository.saveAll(requests);

		logger.info("Bulk activity approval completed successfully. RequestedCount={}, ApprovedBy={}",
				requestIds.size(), approvedBy);
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Request rejected successfully", request);
	}

	@Override
	public Response rejectSelectedRequests(List<String> requestIds, String reason) {

		logger.info("Bulk rejection started.");

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ActivityUpdateRequest> requests = requestRepository.findAllById(requestIds);

		if (requests.isEmpty()) {

			throw new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "No requests found", null);
		}

		String approvedBy = UserContextUtil.getCurrentUser();

		List<Activity> oldActivities = new ArrayList<>();

		List<Activity> newActivities = new ArrayList<>();

		for (ActivityUpdateRequest request : requests) {

			if (!"PENDING".equals(request.getStatus()))
				continue;

			oldActivities.add(request.getOldActivity());

			newActivities.add(request.getNewActivity());

			request.setStatus("REJECTED");

			request.setRejectionReason(reason);

			request.setApprovedBy(approvedBy);

			request.setApprovedByUserId(UserContextUtil.getCurrentUserId());

			request.setApprovedAt(LocalDateTime.now());

			notificationService.createNotification("Activity Update Rejected",
					"Your update request for activity '" + request.getNewActivityName() + "' has been rejected.",
					"ACTIVITY_REJECTED", request.getId(), "/tasks", request.getRequestedByUserId());
		}

		requestRepository.saveAll(requests);

		auditService.saveAuditLog(AuditAction.REJECTED_ALL_ACTIVITY_UPDATES, AuditEntity.ACTIVITY, "Bulk Rejection",
				null, oldActivities, newActivities, approvedBy);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				requests.size() + " requests rejected successfully", requests.size());
	}

	@Override
	public Response rollbackActivity(String auditId) {

		logger.warn("Activity rollback initiated. AuditId={}, RequestedBy={}", auditId,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			AuditLog auditLog = auditLogRepository.findById(auditId).orElseThrow(
					() -> new ResourceNotFoundException(ErrorCode.AUDIT_NOT_FOUND, "Audit log not found", auditId));

			Project project = projectRepository.findByProjectName(auditLog.getProjectName())
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
							auditLog.getProjectName()));

			Activity oldActivity = objectMapper.convertValue(auditLog.getOldData(), Activity.class);

			HierarchyReference ref = HierarchyReferenceUtil.findHierarchy(project, oldActivity.getActivityId());

			BeanUtils.copyProperties(oldActivity, ref.getActivity());



			if ("Implementation User".equalsIgnoreCase(auditLog.getRequestedByRole())) {

				currentActivity.setLocked(false);
				currentActivity.setLockedBy(null);
				currentActivity.setLockedAt(null);

			} else {

				currentActivity.setLocked(true);
			}
			projectRepository.save(project);

			auditService.saveAuditLog(AuditAction.ROLLBACK_ACTIVITY, AuditEntity.ACTIVITY,
					oldActivity.getActivityName(), project.getProjectName(), null, oldActivity,
					UserContextUtil.getCurrentUser());

			logger.info("Rollback completed successfully.");

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Rollback completed successfully", ref.getActivity());

		} catch (Exception ex) {

			logger.error("Rollback failed", ex);

			throw ex;
		}
	}

	@Override
	public Response getAllRequests() {

		logger.info("Fetching all activity update requests.");

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			List<ActivityUpdateRequest> requests = requestRepository.findAllByOrderByRequestedAtDesc();

			logger.info("Successfully fetched {} activity update requests.", requests.size());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Requests fetched successfully.", requests);

		} catch (Exception ex) {

			logger.error("Failed to fetch activity update requests.", ex);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to fetch activity update requests.");
		}
	}

}