package com.novillex.progresstracker.serviceImpl;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
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
	private AuditService auditService;

	@Override
	public Response updateActivity(ActivityModel request) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		WriteUtil.validateRequest(request);
		Project project = projectRepository.findByProjectName(request.getProjectName()).orElse(null);
		if (project == null) {
			logger.warn("Project not found. Project: {}", request.getProjectName());
			throw new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
					request.getProjectName());
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

		if (activityToUpdate == null) {
			logger.warn("Activity not found. Activity: {}, Project: {}", request.getActivityName(),
					request.getProjectName());
			throw new ResourceNotFoundException(ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found",
					request.getActivityName());
		}
		Activity oldActivity = new Activity();

		BeanUtils.copyProperties(activityToUpdate, oldActivity);
		activityToUpdate.setEstimatedPeriodWeek(request.getEstimatedPeriodWeek());
		activityToUpdate.setPlannedStartDate(request.getPlannedStartDate());
		activityToUpdate.setPlannedEndDate(request.getPlannedEndDate());
		activityToUpdate.setActualStartDate(request.getActualStartDate());
		activityToUpdate.setActualEndDate(request.getActualEndDate());
		activityToUpdate.setActualPeriodWeek(
				WriteUtil.calculateActualPeriodWeek(request.getActualStartDate(), request.getActualEndDate()));
		activityToUpdate.setExecutionStatus(WriteUtil.calculateExecutionStatus(request.getProgress()));
		activityToUpdate.setProgress(request.getProgress());
		activityToUpdate.setScheduleHealth(
				WriteUtil.calculateScheduleHealth(request.getProgress(), request.getPlannedStartDate(),
						request.getPlannedEndDate(), request.getActualStartDate(), request.getActualEndDate()));
		activityToUpdate.setRemark(request.getRemark());
				
		if (!isActivityChanged(oldActivity, activityToUpdate)) {
			logger.warn("No changes found for activity: {}", request.getActivityName());

			throw new ValidationException(ErrorCode.NO_CHANGES_FOUND, "No changes found to update");
		}

		projectRepository.save(project);

		String modifiedBy = UserContextUtil.getCurrentUser();

		auditService.saveAuditLog(AuditAction.UPDATE_ACTIVITY, AuditEntity.ACTIVITY, activityToUpdate.getActivityName(),
				project.getProjectName(), oldActivity, activityToUpdate, modifiedBy);

		logger.info("Activity updated successfully. Activity: {}, Project: {}, Modified By: {}",
				activityToUpdate.getActivityName(), project.getProjectName(), modifiedBy);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Activity updated successfully", activityToUpdate);
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
