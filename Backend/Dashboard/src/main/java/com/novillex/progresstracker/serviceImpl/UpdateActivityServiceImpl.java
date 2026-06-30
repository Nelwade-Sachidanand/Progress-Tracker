package com.novillex.progresstracker.serviceImpl;

import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.service.UpdateActivityService;
import com.novillex.progresstracker.util.UserContextUtil;
import com.novillex.progresstracker.util.WriteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UpdateActivityServiceImpl implements UpdateActivityService {

	private static final Logger logger = LoggerFactory.getLogger(UpdateActivityServiceImpl.class);
	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ActivityUpdateRequestRepository requestRepository;

	@Autowired
	private AuditService auditService;

	@Autowired
	private NotificationService notificationService;

	@Override
	public Response updateActivity(ActivityModel request) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		WriteUtil.validateRequest(request);
		Project project = projectRepository.findById(request.getProjectId()).orElse(null);
		if (project == null) {
			logger.warn("Project not found. ProjectId: {}", request.getProjectId());
			throw new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
					request.getProjectId());
		}
		Activity activityToUpdate = null;

		for (Phase phase : project.getPhases()) {
			if (!phase.getPhaseName().equals(request.getPhaseName())) {
				continue;
			}

			for (Milestone milestone : phase.getMilestones()) {
				if (!milestone.getMilestoneName().equals(request.getMilestoneName())) {
					continue;
				}

				for (Task task : milestone.getTasks()) {
					if (!task.getTaskName().equals(request.getTaskName())) {
						continue;
					}

					for (Subtask subTask : task.getSubTasks()) {
						if (!subTask.getSubTaskName().equals(request.getSubTaskName())) {
							continue;
						}
						if (subTask.getActivities() != null) {
							for (Activity activity : subTask.getActivities()) {
								if (activity.getActivityName().equals(request.getActivityName())) {

									activityToUpdate = activity;
									break;
								}
							}
						}
					}
				}
			}
		}

		if (activityToUpdate == null) {
			logger.warn("Activity not found. Activity: {}, ProjectId: {}", request.getActivityName(),
					request.getProjectId());
			throw new ResourceNotFoundException(ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found",
					request.getActivityName());
		}
		Activity oldActivity = new Activity();

		BeanUtils.copyProperties(activityToUpdate, oldActivity);

		Activity newActivity = new Activity();

		newActivity.setActivityName(request.getActivityName());

		newActivity.setEstimatedPeriodWeek(request.getEstimatedPeriodWeek());

		newActivity.setPlannedStartDate(request.getPlannedStartDate());

		newActivity.setPlannedEndDate(request.getPlannedEndDate());

		newActivity.setActualStartDate(request.getActualStartDate());

		newActivity.setActualEndDate(request.getActualEndDate());

		newActivity.setActualPeriodWeek(
				WriteUtil.calculateActualPeriodWeek(request.getActualStartDate(), request.getActualEndDate()));

		newActivity.setProgress(request.getProgress());

		newActivity.setExecutionStatus(WriteUtil.calculateExecutionStatus(request.getProgress()));

		newActivity.setScheduleHealth(
				WriteUtil.calculateScheduleHealth(request.getProgress(), request.getPlannedStartDate(),
						request.getPlannedEndDate(), request.getActualStartDate(), request.getActualEndDate()));

		newActivity.setRemark(request.getRemark());

		if (!isActivityChanged(oldActivity, newActivity)) {

			logger.warn("No changes found for activity: {}", request.getActivityName());

			throw new ValidationException(ErrorCode.NO_CHANGES_FOUND, "No changes found to update");
		}

		ActivityUpdateRequest activityRequest = new ActivityUpdateRequest();

		activityRequest.setProjectId(request.getProjectId());

		activityRequest.setPhaseName(request.getPhaseName());

		activityRequest.setMilestoneName(request.getMilestoneName());

		activityRequest.setTaskName(request.getTaskName());

		activityRequest.setSubTaskName(request.getSubTaskName());

		activityRequest.setActivityName(request.getActivityName());

		activityRequest.setOldActivity(oldActivity);

		activityRequest.setNewActivity(newActivity);

		activityRequest.setRequestedBy(UserContextUtil.getCurrentUser());

		activityRequest.setStatus("PENDING");

		activityRequest.setRequestedAt(java.time.LocalDateTime.now());
		activityRequest.setRequestSource("MANUAL");

		ActivityUpdateRequest existingRequest = requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(
						request.getProjectId(), request.getPhaseName(), request.getMilestoneName(),
						request.getTaskName(), request.getSubTaskName(), request.getActivityName(), "PENDING")
				.orElse(null);

		if (existingRequest != null) {

			throw new ValidationException(ErrorCode.REQUEST_ALREADY_PENDING,
					"Activity update request already pending for approval");
		}

		requestRepository.save(activityRequest);

		notificationService.createNotification("Activity Update Requested",
				UserContextUtil.getCurrentUser() + " requested update for activity " + request.getActivityName(),
				"ACTIVITY_UPDATE", activityRequest.getId(), "/authorization");

		auditService.saveAuditLog(AuditAction.REQUEST_ACTIVITY_UPDATE, AuditEntity.ACTIVITY, request.getActivityName(),
				project.getProjectName(), oldActivity, newActivity, UserContextUtil.getCurrentUser());

		logger.info("Activity update request submitted successfully. Activity: {}, Project: {}",
				request.getActivityName(), project.getProjectName());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Activity update request submitted successfully. Waiting for admin approval.", activityRequest);
	}

	private boolean isActivityChanged(Activity oldActivity, Activity newActivity) {
		return !Objects.equals(oldActivity.getEstimatedPeriodWeek(), newActivity.getEstimatedPeriodWeek())
				|| !Objects.equals(oldActivity.getPlannedStartDate(), newActivity.getPlannedStartDate())
				|| !Objects.equals(oldActivity.getPlannedEndDate(), newActivity.getPlannedEndDate())
				|| !Objects.equals(oldActivity.getActualStartDate(), newActivity.getActualStartDate())
				|| !Objects.equals(oldActivity.getActualEndDate(), newActivity.getActualEndDate())
				|| !Objects.equals(oldActivity.getActualPeriodWeek(), newActivity.getActualPeriodWeek())
				|| !Objects.equals(oldActivity.getProgress(), newActivity.getProgress())
				|| !Objects.equals(oldActivity.getRemark(), newActivity.getRemark());
	}

}
