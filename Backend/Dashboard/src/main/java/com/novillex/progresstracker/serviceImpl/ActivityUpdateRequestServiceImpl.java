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
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.ActivityHistory;
import com.novillex.progresstracker.entity.ActivityUpdateRequest;
import com.novillex.progresstracker.entity.AuditLog;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.repository.ActivityHistoryRepository;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.AuditLogRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.ActivityUpdateRequestService;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
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

		List<ActivityUpdateRequest> requests = requestRepository.findByStatus("PENDING");

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Pending requests fetched successfully", requests);
	}

	@Override
	public Response approveRequest(String requestId) {

		logger.info("Activity update approval initiated. RequestId={}, RequestedBy={}", requestId,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			ActivityUpdateRequest request = requestRepository.findById(requestId).orElseThrow(
					() -> new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Request not found", requestId));

			Project project = projectRepository.findById(request.getProjectId())
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
							request.getProjectId()));

			Activity activity = findActivity(project, request);

			BeanUtils.copyProperties(request.getNewActivity(), activity);

			projectRepository.save(project);

			request.setStatus("APPROVED");
			request.setApprovedBy(UserContextUtil.getCurrentUser());
			request.setApprovedAt(LocalDateTime.now());

			requestRepository.save(request);

			notificationService.createNotification("Activity Update Approved",
					"Your update request for activity '" + request.getActivityName() + "' has been approved.",
					"ACTIVITY_APPROVED", request.getId(), "/tasks", request.getRequestedByUserId());

			auditService.saveAuditLog(AuditAction.APPROVE_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
					request.getActivityName(), project.getProjectName(), request.getOldActivity(),
					request.getNewActivity(), UserContextUtil.getCurrentUser());

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
	}

	@Override
	public Response rejectRequest(String requestId, String reason) {

		logger.info("Activity update rejection initiated. RequestId={}, RejectedBy={}", requestId,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			ActivityUpdateRequest request = requestRepository.findById(requestId).orElseThrow(
					() -> new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Request not found", requestId));

			request.setStatus("REJECTED");
			request.setRejectionReason(reason);
			request.setApprovedBy(UserContextUtil.getCurrentUser());
			request.setApprovedAt(LocalDateTime.now());

			requestRepository.save(request);
			
			notificationService.createNotification("Activity Update Rejected",
					"Your update request for activity '" + request.getActivityName() + "' has been rejected.",
					"ACTIVITY_REJECTED", request.getId(), "/tasks", request.getRequestedByUserId());

			auditService.saveAuditLog(AuditAction.REJECT_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
					request.getActivityName(), null, request.getOldActivity(), request.getNewActivity(),
					UserContextUtil.getCurrentUser());

			logger.info("Activity update rejected successfully. RequestId={}, ActivityName={}, RejectedBy={}",
					requestId, request.getActivityName(), UserContextUtil.getCurrentUser());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Request rejected successfully", request);

		} catch (ResourceNotFoundException ex) {

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

			projectRepository.save(project);

			request.setStatus("APPROVED");
			request.setApprovedBy(approvedBy);
			request.setApprovedAt(LocalDateTime.now());
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
	public Response rejectSelectedRequests(List<String> requestIds, String reason) {

		logger.info("Bulk activity rejection initiated. TotalRequestIds={}, RejectedBy={}", requestIds.size(),
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ActivityUpdateRequest> requests = requestRepository.findAllById(requestIds);

		if (requests.isEmpty()) {
			logger.warn("Bulk activity rejection failed. No requests found. RequestIds={}", requestIds);
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

			request.setStatus("REJECTED");
			request.setRejectionReason(reason);
			request.setApprovedBy(approvedBy);
			request.setApprovedAt(LocalDateTime.now());
		}

		requestRepository.saveAll(requests);

		auditService.saveAuditLog(AuditAction.REJECTED_ALL_ACTIVITY_UPDATES, AuditEntity.ACTIVITY,
				"Bulk Rejection (" + requests.size() + " Requests)", null, oldActivities, newActivities, approvedBy);
		logger.info("Bulk activity rejection completed successfully.RequestedCount={}, RejectedBy={}",
				requestIds.size(), approvedBy);
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

			Activity currentActivity = findActivity(project, oldActivity);

			BeanUtils.copyProperties(oldActivity, currentActivity);

			projectRepository.save(project);

			// Recommended audit log
			auditService.saveAuditLog(AuditAction.ROLLBACK_ACTIVITY, AuditEntity.ACTIVITY,
					currentActivity.getActivityName(), project.getProjectName(), null, oldActivity,
					UserContextUtil.getCurrentUser());

			logger.warn(
					"Activity rollback completed successfully. AuditId={}, ProjectName={}, ActivityName={}, RolledBackBy={}",
					auditId, project.getProjectName(), currentActivity.getActivityName(),
					UserContextUtil.getCurrentUser());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Activity rolled back successfully", currentActivity);

		} catch (ResourceNotFoundException ex) {

			logger.error("Activity rollback failed. AuditId={}, Reason={}", auditId, ex.getMessage());

			throw ex;

		} catch (Exception ex) {

			logger.error("Unexpected error during activity rollback. AuditId={}", auditId, ex);

			throw ex;
		}
	}

	private Activity findActivity(Project project, ActivityUpdateRequest request) {

		for (Phase phase : project.getPhases()) {

			if (!phase.getPhaseName().equals(request.getPhaseName()))
				continue;

			for (Milestone milestone : phase.getMilestones()) {

				if (!milestone.getMilestoneName().equals(request.getMilestoneName()))
					continue;

				for (Task task : milestone.getTasks()) {

					if (!task.getTaskName().equals(request.getTaskName()))
						continue;

					for (Subtask subTask : task.getSubTasks()) {

						if (!subTask.getSubTaskName().equals(request.getSubTaskName()))
							continue;

						for (Activity activity : subTask.getActivities()) {

							if (activity.getActivityName().equals(request.getActivityName())) {

								return activity;
							}
						}
					}
				}
			}
		}

		throw new ResourceNotFoundException(ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found",
				request.getActivityName());
	}

	@Override
	public Response getAllRequests() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			List<ActivityUpdateRequest> requests = requestRepository.findAll();

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Requests Fetched Successfully", requests);

		} catch (Exception ex) {

			logger.error("Failed to fetch activity update requests.", ex);

			throw ex;
		}
	}

	private Activity findActivity(Project project, Activity activityToFind) {

		for (Phase phase : project.getPhases()) {

			for (Milestone milestone : phase.getMilestones()) {

				for (Task task : milestone.getTasks()) {

					for (Subtask subTask : task.getSubTasks()) {

						for (Activity activity : subTask.getActivities()) {

							if (activity.getActivityName().equals(activityToFind.getActivityName())) {

								return activity;
							}
						}
					}
				}
			}
		}

		throw new ResourceNotFoundException(ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found",
				activityToFind.getActivityName());
	}

}