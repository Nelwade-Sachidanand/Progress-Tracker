package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.novillex.progresstracker.entity.Project;
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

	private ActivityUpdateRequestRepository requestRepository;

	private ProjectRepository projectRepository;

	private ApplicationContext context;

	private AuditService auditService;

	private AuditLogRepository auditLogRepository;

	private NotificationService notificationService;

	private ObjectMapper objectMapper;

	private static final Logger logger = LoggerFactory.getLogger(ActivityUpdateRequestServiceImpl.class);

	public ActivityUpdateRequestServiceImpl(ActivityUpdateRequestRepository requestRepository,
			ProjectRepository projectRepository, ApplicationContext context, AuditService auditService,
			AuditLogRepository auditLogRepository, NotificationService notificationService, ObjectMapper objectMapper) {

		this.requestRepository = requestRepository;
		this.projectRepository = projectRepository;
		this.context = context;
		this.auditService = auditService;
		this.auditLogRepository = auditLogRepository;
		this.notificationService = notificationService;
		this.objectMapper = objectMapper;
	}

	@Override
	public Response getPendingRequests() {

		logger.info("Fetching pending activity update requests.");

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			List<ActivityUpdateRequest> requests = requestRepository.findByStatus("PENDING");

			logger.info("Successfully fetched {} pending activity update requests.", requests.size());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Pending requests fetched successfully.", requests);

		} catch (Exception ex) {

			logger.error("Failed to fetch pending activity update requests.", ex);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to fetch pending activity update requests.");
		}
	}

	@Transactional
	@Override
	public Response approveRequest(String requestId) {

		logger.info("Activity update approval initiated. RequestId={}, ApprovedBy={}", requestId,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ActivityUpdateRequest request = requestRepository.findById(requestId).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Request not found", requestId));

		if (!"PENDING".equalsIgnoreCase(request.getStatus())) {

			throw new IllegalStateException("Only pending requests can be approved.");
		}

		LocalDateTime approvedAt = LocalDateTime.now();

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

			ref.getActivity().setLocked(true);

			ref.getActivity().setLockedBy(UserContextUtil.getCurrentUser());

			ref.getActivity().setLockedAt(approvedAt);

		} else {

			ref.getActivity().setLocked(false);

			ref.getActivity().setLockedBy(null);

			ref.getActivity().setLockedAt(null);
		}

		projectRepository.save(project);

		request.setStatus("APPROVED");

		request.setApprovedBy(UserContextUtil.getCurrentUser());

		request.setApprovedByUserId(UserContextUtil.getCurrentUserId());

		request.setApprovedAt(approvedAt);

		requestRepository.save(request);

		notificationService.createNotification("Activity Update Approved",
				"Your update request for activity '" + request.getNewActivityName() + "' has been approved.",
				"ACTIVITY_APPROVED", request.getId(), "/tasks", request.getRequestedByUserId());

		auditService.saveAuditLog(AuditAction.APPROVE_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
				request.getNewActivityName(), project.getProjectName(), request.getOldActivity(),
				request.getNewActivity(), UserContextUtil.getCurrentUser(), request.getRequestedByRole());

		logger.info("Activity update approved successfully. RequestId={}, Activity={}", requestId,
				request.getNewActivityName());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Request approved successfully", request);
	}

	@Transactional
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

		LocalDateTime approvedAt = LocalDateTime.now();

		List<Activity> oldActivities = new ArrayList<>();

		List<Activity> newActivities = new ArrayList<>();

		int approvedCount = 0;

		for (ActivityUpdateRequest request : requests) {

			if (!"PENDING".equalsIgnoreCase(request.getStatus())) {

				logger.warn("Skipping RequestId={} because status is {}", request.getId(), request.getStatus());

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

				ref.getActivity().setLocked(true);

				ref.getActivity().setLockedBy(approvedBy);

				ref.getActivity().setLockedAt(approvedAt);

			} else {

				ref.getActivity().setLocked(false);

				ref.getActivity().setLockedBy(null);

				ref.getActivity().setLockedAt(null);
			}

			projectRepository.save(project);

			request.setStatus("APPROVED");

			request.setApprovedBy(approvedBy);

			request.setApprovedByUserId(UserContextUtil.getCurrentUserId());

			request.setApprovedAt(approvedAt);

			notificationService.createNotification("Activity Update Approved",
					"Your update request for activity '" + request.getNewActivityName() + "' has been approved.",
					"ACTIVITY_APPROVED", request.getId(), "/tasks", request.getRequestedByUserId());

			auditService.saveAuditLog(AuditAction.APPROVE_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
					request.getNewActivityName(), project.getProjectName(), request.getOldActivity(),
					request.getNewActivity(), approvedBy, request.getRequestedByRole());

			approvedCount++;
		}

		requestRepository.saveAll(requests);

		logger.info("Bulk activity approval completed successfully. RequestedCount={}, ApprovedCount={}, ApprovedBy={}",
				requestIds.size(), approvedCount, approvedBy);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				approvedCount + " requests approved successfully", approvedCount);
	}

	@Transactional
	@Override
	public Response rejectRequest(String requestId, String reason) {

		logger.info("Activity update rejection initiated. RequestId={}, RejectedBy={}", requestId,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ActivityUpdateRequest request = requestRepository.findById(requestId).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Request not found", requestId));

		if (!"PENDING".equalsIgnoreCase(request.getStatus())) {

			throw new IllegalStateException("Only pending requests can be rejected.");
		}

		if (reason == null || reason.trim().isEmpty()) {

			throw new IllegalArgumentException("Rejection reason is required.");
		}

		LocalDateTime rejectedAt = LocalDateTime.now();

		String rejectedBy = UserContextUtil.getCurrentUser();

		request.setStatus("REJECTED");

		request.setRejectionReason(reason.trim());

		request.setApprovedBy(rejectedBy);

		request.setApprovedByUserId(UserContextUtil.getCurrentUserId());

		request.setApprovedAt(rejectedAt);

		requestRepository.save(request);

		notificationService.createNotification("Activity Update Rejected",
				"Your update request for activity '" + request.getNewActivityName() + "' has been rejected.",
				"ACTIVITY_REJECTED", request.getId(), "/tasks", request.getRequestedByUserId());

		auditService.saveAuditLog(AuditAction.REJECT_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
				request.getNewActivityName(), request.getProjectName(), request.getOldActivity(),
				request.getNewActivity(), rejectedBy);

		logger.info("Activity update rejected successfully. RequestId={}, Activity={}", requestId,
				request.getNewActivityName());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Request rejected successfully", request);
	}

	@Transactional
	@Override
	public Response rejectSelectedRequests(List<String> requestIds, String reason) {

		logger.info("Bulk activity rejection initiated. TotalRequestIds={}, RejectedBy={}", requestIds.size(),
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ActivityUpdateRequest> requests = requestRepository.findAllById(requestIds);

		if (requests.isEmpty()) {

			throw new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "No requests found", null);
		}

		if (reason == null || reason.trim().isEmpty()) {

			throw new IllegalArgumentException("Rejection reason is required.");
		}

		String rejectedBy = UserContextUtil.getCurrentUser();

		LocalDateTime rejectedAt = LocalDateTime.now();

		List<Activity> oldActivities = new ArrayList<>();

		List<Activity> newActivities = new ArrayList<>();

		int rejectedCount = 0;

		for (ActivityUpdateRequest request : requests) {

			if (!"PENDING".equalsIgnoreCase(request.getStatus())) {

				logger.warn("Skipping RequestId={} because status is {}", request.getId(), request.getStatus());

				continue;
			}

			oldActivities.add(request.getOldActivity());

			newActivities.add(request.getNewActivity());

			request.setStatus("REJECTED");

			request.setRejectionReason(reason.trim());

			request.setApprovedBy(rejectedBy);

			request.setApprovedByUserId(UserContextUtil.getCurrentUserId());

			request.setApprovedAt(rejectedAt);

			notificationService.createNotification("Activity Update Rejected",
					"Your update request for activity '" + request.getNewActivityName() + "' has been rejected.",
					"ACTIVITY_REJECTED", request.getId(), "/tasks", request.getRequestedByUserId());

			rejectedCount++;
		}

		requestRepository.saveAll(requests);

		auditService.saveAuditLog(AuditAction.REJECTED_ALL_ACTIVITY_UPDATES, AuditEntity.ACTIVITY, "Bulk Rejection",
				null, oldActivities, newActivities, rejectedBy);

		logger.info(
				"Bulk activity rejection completed successfully. RequestedCount={}, RejectedCount={}, RejectedBy={}",
				requestIds.size(), rejectedCount, rejectedBy);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				rejectedCount + " requests rejected successfully", rejectedCount);
	}

	@Transactional
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

				ref.getActivity().setLocked(false);
				ref.getActivity().setLockedBy(null);
				ref.getActivity().setLockedAt(null);

			} else {

				ref.getActivity().setLocked(true);
				ref.getActivity().setLockedBy(UserContextUtil.getCurrentUser());
				ref.getActivity().setLockedAt(LocalDateTime.now());
			}

			projectRepository.save(project);

			auditService.saveAuditLog(AuditAction.ROLLBACK_ACTIVITY, AuditEntity.ACTIVITY,
					oldActivity.getActivityName(), project.getProjectName(), null, oldActivity,
					UserContextUtil.getCurrentUser(), auditLog.getRequestedByRole());

			logger.info("Rollback completed successfully. AuditId={}, Activity={}", auditId,
					oldActivity.getActivityName());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Rollback completed successfully", ref.getActivity());

		} catch (Exception ex) {

			logger.error("Rollback failed. AuditId={}", auditId, ex);

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