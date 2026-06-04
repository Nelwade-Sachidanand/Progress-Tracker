package com.dashboard.serviceImpl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.dashboard.common.AuditAction;
import com.dashboard.common.AuditEntity;
import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Activity;
import com.dashboard.entity.Milestone;
import com.dashboard.entity.Phase;
import com.dashboard.entity.Project;
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.exception.ResourceNotFoundException;
import com.dashboard.exception.ValidationException;
import com.dashboard.model.ActivityModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.AuditService;
import com.dashboard.service.UpdateActivityService;
import com.dashboard.util.UserContextUtil;
import com.dashboard.util.WriteUtil;

@Service
public class UpdateActivityServiceImpl implements UpdateActivityService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private AuditService auditService;

	@Override
	public Response updateActivity(ActivityModel request) {

		// System.out.println(request);
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		
		WriteUtil.validateRequest(request);
		Project project = projectRepository.findByProjectName(request.getProjectName()).orElse(null);
		if(project == null) {
			throw new ResourceNotFoundException("PRJ_404", "Project not found", request.getProjectName());
		}
		Activity activityToUpdate = null;

		for(Phase phase : project.getPhases()) {
			if(!phase.getPhaseName().equals(request.getPhaseName())) {
				continue;
			}

			for(Milestone milestone : phase.getMilestones()) {
				if(!milestone.getMilestoneName().equals(request.getMilestoneName())) {
					continue;
				}

				for(Task task : milestone.getTasks()) {
					if(!task.getTaskName().equals(request.getTaskName())) {
						continue;
					}

					for(Subtask subTask : task.getSubTasks()) {
						if(!subTask.getSubTaskName().equals(request.getSubTaskName())) {
							continue;
						}

						for(Activity activity : subTask.getActivities()) {
							if(activity.getActivityName().equals(request.getActivityName())) {

								activityToUpdate = activity;
								break;
							}
						}
					}
				}
			}
		}

		if(activityToUpdate == null) {
			throw new ResourceNotFoundException("ACT_404", "Activity not found", request.getActivityName());
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
		activityToUpdate.setScheduleHealth(WriteUtil.calculateScheduleHealth(request.getProgress(), request.getPlannedStartDate(),
				request.getPlannedEndDate(), request.getActualStartDate(), request.getActualEndDate()));

		// System.out.println(request);
		if(!isActivityChanged(oldActivity, activityToUpdate)) {
			throw new ValidationException("VAL_020", "No changes found to update");
		}

		projectRepository.save(project);

		String modifiedBy = UserContextUtil.getCurrentUser();
		// System.out.println("Logged In User : " + username);
		auditService.saveAuditLog(AuditAction.UPDATE_ACTIVITY, AuditEntity.ACTIVITY, activityToUpdate.getActivityName(),
				project.getProjectName(), oldActivity, activityToUpdate, modifiedBy);

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
				|| !Objects.equals(oldActivity.getProgress(), newActivity.getProgress());
	}

	
}
