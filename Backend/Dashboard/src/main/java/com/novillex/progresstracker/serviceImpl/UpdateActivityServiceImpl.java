package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
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
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.ActivityUpdateRequestModel;
import com.novillex.progresstracker.model.AddRemarkModel;
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

	private ProjectRepository projectRepository;

	private ApplicationContext context;

	private ActivityUpdateRequestRepository requestRepository;

	private AuditService auditService;

	private NotificationService notificationService;

	public UpdateActivityServiceImpl(ProjectRepository projectRepository, ApplicationContext context,
			AuditService auditService, ActivityUpdateRequestRepository requestRepository,
			NotificationService notificationService) {
		this.projectRepository = projectRepository;
		this.context = context;
		this.requestRepository = requestRepository;
		this.auditService = auditService;
		this.notificationService = notificationService;

	}

	private boolean isActivityChanged(Activity oldActivity, Activity newActivity) {

		return !Objects.equals(oldActivity.getActivityName(), newActivity.getActivityName())
				|| !Objects.equals(oldActivity.getOwner(), newActivity.getOwner())
				|| !Objects.equals(oldActivity.getEstimatedPeriodWeek(), newActivity.getEstimatedPeriodWeek())
				|| !Objects.equals(oldActivity.getPlannedStartDate(), newActivity.getPlannedStartDate())
				|| !Objects.equals(oldActivity.getPlannedEndDate(), newActivity.getPlannedEndDate())
				|| !Objects.equals(oldActivity.getActualStartDate(), newActivity.getActualStartDate())
				|| !Objects.equals(oldActivity.getActualEndDate(), newActivity.getActualEndDate())
				|| !Objects.equals(oldActivity.getActualPeriodWeek(), newActivity.getActualPeriodWeek())
				|| !Objects.equals(oldActivity.getProgress(), newActivity.getProgress())
				|| !Objects.equals(oldActivity.getExecutionStatus(), newActivity.getExecutionStatus())
				|| !Objects.equals(oldActivity.getScheduleHealth(), newActivity.getScheduleHealth())
				|| !Objects.equals(oldActivity.getRemark(), newActivity.getRemark());
	}

	@Override
	public Response updateActivityRequest(ActivityUpdateRequestModel request) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		WriteUtil.validateUpdateRequest(request);

		Project project = projectRepository
				.findByPhasesMilestonesTasksSubTasksActivitiesActivityId(request.getActivityId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
						request.getProjectId()));

		Phase phaseToUpdate = null;
		Milestone milestoneToUpdate = null;
		Task taskToUpdate = null;
		Subtask subTaskToUpdate = null;
		Activity activityToUpdate = null;

		outerLoop: for (Phase phase : project.getPhases()) {

			for (Milestone milestone : phase.getMilestones()) {

				if (milestone.getWeightage() == null || milestone.getWeightage() <= 0) {

					logger.warn("Milestone weightage is not defined. Milestone={}", milestone.getMilestoneName());

					throw new ValidationException(ErrorCode.MILESTONE_WEIGHTAGE_NOT_DEFINED,
							"Milestone weightage is not defined. Please assign milestone weightage first.");
				}

				for (Task task : milestone.getTasks()) {

					for (Subtask subTask : task.getSubTasks()) {

						if (subTask.getActivities() == null) {
							continue;
						}

						for (Activity activity : subTask.getActivities()) {

							if (activity.getActivityId().equals(request.getActivityId())) {

								phaseToUpdate = phase;
								milestoneToUpdate = milestone;
								taskToUpdate = task;
								subTaskToUpdate = subTask;
								activityToUpdate = activity;

								break outerLoop;
							}
						}
					}
				}
			}
		}

		if (activityToUpdate == null) {

			logger.warn("Activity not found. ActivityId={}", request.getActivityId());

			throw new ResourceNotFoundException(ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found",
					request.getActivityId());
		}

		if (Boolean.TRUE.equals(activityToUpdate.getLocked())
				&& !"ADMIN".equalsIgnoreCase(UserContextUtil.getCurrentUserRole())) {

			logger.warn("Update denied. Activity '{}' is locked. User '{}' attempted modification.",
					activityToUpdate.getActivityName(), UserContextUtil.getCurrentUser());

			throw new ValidationException(ErrorCode.ACTIVITY_LOCKED,
					"This activity has already been approved. Only Admin can update it.");
		}

		String oldPhaseName = phaseToUpdate.getPhaseName();
		String oldMilestoneName = milestoneToUpdate.getMilestoneName();
		String oldTaskName = taskToUpdate.getTaskName();
		String oldSubTaskName = subTaskToUpdate.getSubTaskName();

		Activity oldActivity = new Activity();
		BeanUtils.copyProperties(activityToUpdate, oldActivity);

		String newPhaseName = request.getPhaseName();
		String newMilestoneName = request.getMilestoneName();
		String newTaskName = request.getTaskName();
		String newSubTaskName = request.getSubTaskName();

		Activity newActivity = new Activity();

		newActivity.setActivityId(activityToUpdate.getActivityId());

		newActivity.setActivityName(request.getActivityName());

		newActivity.setOwner(request.getOwner());

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
						request.getPlannedEndDate(), request.getActualStartDate(), request.getActualEndDate(),
						WriteUtil.calculateActualPeriodWeek(request.getActualStartDate(), request.getActualEndDate())));
		newActivity.setRemark(oldActivity.getRemark());

		boolean hierarchyChanged = !Objects.equals(oldPhaseName, newPhaseName)
				|| !Objects.equals(oldMilestoneName, newMilestoneName) || !Objects.equals(oldTaskName, newTaskName)
				|| !Objects.equals(oldSubTaskName, newSubTaskName);

		boolean activityChanged = isActivityChanged(oldActivity, newActivity);

		if (!hierarchyChanged && !activityChanged) {

			logger.warn("No changes found for activity {}", request.getActivityId());

			throw new ValidationException(ErrorCode.NO_CHANGES_FOUND, "No changes found to update");
		}

		ActivityUpdateRequest existingRequest = requestRepository
				.findByActivityIdAndStatus(request.getActivityId(), "PENDING").orElse(null);

		if (existingRequest != null) {

			throw new ValidationException(ErrorCode.REQUEST_ALREADY_PENDING,
					"Activity update request already pending for approval");
		}

		ActivityUpdateRequest activityRequest = new ActivityUpdateRequest();

		activityRequest.setProjectId(project.getId());
		activityRequest.setProjectName(project.getProjectName());

		activityRequest.setPhaseId(phaseToUpdate.getPhaseId());
		activityRequest.setMilestoneId(milestoneToUpdate.getMilestoneId());
		activityRequest.setTaskId(taskToUpdate.getTaskId());
		activityRequest.setSubTaskId(subTaskToUpdate.getSubTaskId());
		activityRequest.setActivityId(activityToUpdate.getActivityId());

		activityRequest.setOldPhaseName(oldPhaseName);
		activityRequest.setOldMilestoneName(oldMilestoneName);
		activityRequest.setOldTaskName(oldTaskName);
		activityRequest.setOldSubTaskName(oldSubTaskName);

		activityRequest.setOldOwner(oldActivity.getOwner());
		activityRequest.setOldActivityName(oldActivity.getActivityName());

		activityRequest.setNewPhaseName(request.getPhaseName());
		activityRequest.setNewMilestoneName(request.getMilestoneName());
		activityRequest.setNewTaskName(request.getTaskName());
		activityRequest.setNewSubTaskName(request.getSubTaskName());

		activityRequest.setNewOwner(request.getOwner());
		activityRequest.setNewActivityName(request.getActivityName());

		activityRequest.setOldActivity(oldActivity);
		activityRequest.setNewActivity(newActivity);

		activityRequest.setRequestSource("MANUAL");
		activityRequest.setRequestType("UPDATE");
		activityRequest.setStatus("PENDING");

		activityRequest.setChangeReason(request.getChangeReason());

		activityRequest.setRequestedBy(UserContextUtil.getCurrentUser());
		activityRequest.setRequestedByUserId(UserContextUtil.getCurrentUserId());
		activityRequest.setRequestedAt(LocalDateTime.now());
		activityRequest.setRequestedByRole(UserContextUtil.getCurrentUserRole());

		requestRepository.save(activityRequest);

//		notificationService.createNotification("Activity Update Requested",
//				UserContextUtil.getCurrentUser() + " Requested Update for Activity "
//						+ activityRequest.getNewActivityName(),
//				"ACTIVITY_UPDATE", activityRequest.getId(), "/authorization", null);

		notificationService.createNotification("Activity Update Requested",
				UserContextUtil.getCurrentUser() + " Requested Update for Activity "
						+ activityRequest.getNewActivityName(),
				"ACTIVITY_UPDATE", activityRequest.getId(),
				"/authorization?type=activity-update&requestId=" + activityRequest.getId(), null);

		auditService.saveAuditLog(AuditAction.REQUEST_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
				activityRequest.getNewActivityName(), project.getProjectName(), oldActivity, newActivity,
				UserContextUtil.getCurrentUser());

		logger.info("Activity update request submitted successfully. Activity={}, Project={}",
				request.getActivityName(), project.getProjectName());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Activity update request submitted successfully. Waiting for admin approval.", activityRequest);
	}

	@Override
	public Response addRemark(AddRemarkModel model) {
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		logger.info("Add remark request received for projectId: {}, activityId: {}", model.getProjectId(),
				model.getActivityId());

		Project project = projectRepository.findById(model.getProjectId()).orElseThrow(() -> {
			logger.error("Project not found with id: {}", model.getProjectId());
			return new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
					model.getProjectId());
		});

		boolean activityFound = false;

		for (Phase phase : project.getPhases()) {

			if (!phase.getPhaseId().equals(model.getPhaseId())) {
				continue;
			}

			for (Milestone milestone : phase.getMilestones()) {

				if (!milestone.getMilestoneId().equals(model.getMilestoneId())) {
					continue;
				}

				for (Task task : milestone.getTasks()) {

					if (!task.getTaskId().equals(model.getTaskId())) {
						continue;
					}

					for (Subtask subTask : task.getSubTasks()) {

						if (!subTask.getSubTaskId().equals(model.getSubTaskId())) {
							continue;
						}

						for (Activity activity : subTask.getActivities()) {

							if (!activity.getActivityId().equals(model.getActivityId())) {
								continue;
							}

							String existingRemark = activity.getRemark();

							if (existingRemark == null || existingRemark.isBlank()) {
								activity.setRemark(model.getRemark());
							} else {
								activity.setRemark(existingRemark + System.lineSeparator() + model.getRemark());
							}

							activityFound = true;

							logger.info("Remark added successfully for activityId '{}'", activity.getActivityId());

							break;
						}

						if (activityFound) {
							break;
						}
					}

					if (activityFound) {
						break;
					}
				}

				if (activityFound) {
					break;
				}
			}

			if (activityFound) {
				break;
			}
		}

		if (!activityFound) {

			logger.error("Activity not found. ActivityId={}", model.getActivityId());

			throw new ResourceNotFoundException(ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found",
					model.getActivityId());
		}

		projectRepository.save(project);

		logger.info("Project '{}' updated successfully after adding remark.", project.getProjectName());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Remark added successfully", null);

	}

}
