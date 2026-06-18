package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.ActivityUpdateRequest;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.ActivityUpdateRequestService;
import com.novillex.progresstracker.service.AuditService;
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

	@Override
	public Response approveAllRequests() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ActivityUpdateRequest> requests = requestRepository.findByStatus("PENDING");

		if (requests.isEmpty()) {

			throw new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "No pending requests found", null);
		}

		String approvedBy = UserContextUtil.getCurrentUser();

		List<Activity> oldActivities = new ArrayList<>();
		List<Activity> newActivities = new ArrayList<>();

		for (ActivityUpdateRequest request : requests) {

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
				"Bulk Approval (" + requests.size() + " Requests)", null, oldActivities, newActivities,
				UserContextUtil.getCurrentUser());
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				requests.size() + " requests approved successfully", requests.size());
	}

	@Override
	public Response rejectAllRequests(String reason) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ActivityUpdateRequest> requests = requestRepository.findByStatus("PENDING");

		if (requests.isEmpty()) {

			throw new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "No pending requests found", null);
		}

		String approvedBy = UserContextUtil.getCurrentUser();

		List<Activity> oldActivities = new ArrayList<>();
		List<Activity> newActivities = new ArrayList<>();

		for (ActivityUpdateRequest request : requests) {

			oldActivities.add(request.getOldActivity());
			newActivities.add(request.getNewActivity());

			request.setStatus("REJECTED");
			request.setRejectionReason(reason);
			request.setApprovedBy(approvedBy);
			request.setApprovedAt(LocalDateTime.now());

		}

		requestRepository.saveAll(requests);
		auditService.saveAuditLog(AuditAction.REJECTED_ALL_ACTIVITY_UPDATES, AuditEntity.ACTIVITY,
				"Bulk Rejection (" + requests.size() + " Requests)", null, oldActivities, newActivities,
				UserContextUtil.getCurrentUser());
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				requests.size() + " requests rejected successfully", requests.size());
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