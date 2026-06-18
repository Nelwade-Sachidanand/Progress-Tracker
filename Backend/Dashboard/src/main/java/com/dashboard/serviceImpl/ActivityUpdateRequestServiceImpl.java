package com.dashboard.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.dashboard.common.AuditAction;
import com.dashboard.common.AuditEntity;
import com.dashboard.common.ErrorCode;
import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Activity;
import com.dashboard.entity.ActivityUpdateRequest;
import com.dashboard.entity.Milestone;
import com.dashboard.entity.Phase;
import com.dashboard.entity.Project;
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.exception.ResourceNotFoundException;
import com.dashboard.repository.ActivityUpdateRequestRepository;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.ActivityUpdateRequestService;
import com.dashboard.service.AuditService;
import com.dashboard.util.UserContextUtil;

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
	
	

	@Override
	public Response getPendingRequests() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ActivityUpdateRequest> requests = requestRepository.findByStatus("PENDING");

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Pending requests fetched successfully", requests);
	}

	@Override
	public Response approveRequest(String requestId) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

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

		auditService.saveAuditLog(AuditAction.APPROVE_ACTIVITY_UPDATE, AuditEntity.ACTIVITY, request.getActivityName(),
				project.getProjectName(), request.getOldActivity(), request.getNewActivity(),
				UserContextUtil.getCurrentUser());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Request approved successfully", request);
	}

	@Override
	public Response rejectRequest(String requestId, String reason) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ActivityUpdateRequest request = requestRepository.findById(requestId).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Request not found", requestId));

		request.setStatus("REJECTED");

		request.setRejectionReason(reason);

		request.setApprovedBy(UserContextUtil.getCurrentUser());

		request.setApprovedAt(LocalDateTime.now());

		requestRepository.save(request);

		auditService.saveAuditLog(AuditAction.REJECT_ACTIVITY_UPDATE, AuditEntity.ACTIVITY, request.getActivityName(),
				null, request.getOldActivity(), request.getNewActivity(), UserContextUtil.getCurrentUser());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Request rejected successfully", request);
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

}