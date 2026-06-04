package com.dashboard.serviceImpl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import com.dashboard.exception.DatabaseException;
import com.dashboard.exception.ResourceNotFoundException;
import com.dashboard.exception.ValidationException;
import com.dashboard.model.ActivityModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.AuditService;
import com.dashboard.service.CreateStructureService;
import com.dashboard.util.UserContextUtil;
import com.dashboard.util.WriteUtil;

@Service
public class CreateStructureServiceImpl implements CreateStructureService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private AuditService auditService;

	@Override
	public Response createStructure(ActivityModel request) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		WriteUtil.validateRequest(request);

		boolean phaseCreated = false;
		boolean milestoneCreated = false;
		boolean taskCreated = false;
		boolean subtaskCreated = false;
		boolean activityCreated = false;

		Project project = projectRepository.findByProjectName(request.getProjectName()).orElse(null);

		if(project == null) {
			throw new ResourceNotFoundException("PRJ_404", "Project not found", request.getProjectName());
		}
		Phase phase = null;

		if(project.getPhases() != null) {

			for(Phase p : project.getPhases()) {
				if(p.getPhaseName().equalsIgnoreCase(request.getPhaseName())) {
					phase = p;
					break;
				}
			}
		}

		if(phase == null) {

			phase = new Phase();
			phase.setPhaseName(request.getPhaseName());
			phase.setMilestones(new ArrayList<>());

			if(project.getPhases() == null) {
				project.setPhases(new ArrayList<>());
			}

			project.getPhases().add(phase);
			phaseCreated = true;
		}

		Milestone milestone = null;

		if(request.getMilestoneName() != null) {

			for(Milestone m : phase.getMilestones()) {

				if(m.getMilestoneName().equalsIgnoreCase(request.getMilestoneName())) {
					milestone = m;
					break;
				}
			}

			if(milestone == null) {
				milestone = new Milestone();
				milestone.setMilestoneName(request.getMilestoneName());
				milestone.setTasks(new ArrayList<>());
				phase.getMilestones().add(milestone);
				milestoneCreated = true;
			}
		}

		Task task = null;

		if(request.getTaskName() != null) {

			for(Task t : milestone.getTasks()) {

				if(t.getTaskName().equalsIgnoreCase(request.getTaskName())) {
					task = t;
					break;
				}
			}

			if(task == null) {

				task = new Task();
				task.setTaskName(request.getTaskName());
				task.setSubTasks(new ArrayList<>());
				milestone.getTasks().add(task);
				taskCreated = true;
			}
		}

		Subtask subtask = null;

		if(request.getSubTaskName() != null) {

			for(Subtask st : task.getSubTasks()) {

				if(st.getSubTaskName().equalsIgnoreCase(request.getSubTaskName())) {
					subtask = st;
					break;
				}
			}

			if(subtask == null) {
				subtask = new Subtask();
				subtask.setSubTaskName(request.getSubTaskName());
				subtask.setActivities(new ArrayList<>());
				task.getSubTasks().add(subtask);
				subtaskCreated = true;
			}
		}
		Activity activity = null;
		if(request.getActivityName() != null) {

			Activity existingActivity = subtask.getActivities().stream()
					.filter(a -> a.getActivityName().equalsIgnoreCase(request.getActivityName())).findFirst()
					.orElse(null);

			if(existingActivity != null) {

				throw new ValidationException("VAL_006", "Activity already exists");
			}

			activity = new Activity();
			activity.setActivityName(request.getActivityName());
			activity.setEstimatedPeriodWeek(request.getEstimatedPeriodWeek());
			activity.setPlannedStartDate(request.getPlannedStartDate());
			activity.setPlannedEndDate(request.getPlannedEndDate());
			activity.setActualStartDate(request.getActualStartDate());
			activity.setActualEndDate(request.getActualEndDate());

			activity.setActualPeriodWeek(
					WriteUtil.calculateActualPeriodWeek(request.getActualStartDate(), request.getActualEndDate()));

			activity.setProgress(request.getProgress());

			activity.setExecutionStatus(WriteUtil.calculateExecutionStatus(request.getProgress()));

			activity.setScheduleHealth(
					WriteUtil.calculateScheduleHealth(request.getProgress(), request.getPlannedStartDate(),
							request.getPlannedEndDate(), request.getActualStartDate(), request.getActualEndDate()));
			subtask.getActivities().add(activity);
			activityCreated = true;
		}

		try{

			projectRepository.save(project);

		} catch(Exception e) {

			/*
			 * logger.error( "Error while saving project {}", project.getProjectName(), e);
			 */

			throw new DatabaseException("DB_001", "Unable to save project");
		}
		String username = UserContextUtil.getCurrentUser();
		if(phaseCreated) {

			auditService.saveAuditLog(AuditAction.CREATE_PHASE, AuditEntity.PHASE, phase.getPhaseName(),
					project.getProjectName(), null, phase, username);

		} else if(milestoneCreated) {

			auditService.saveAuditLog(AuditAction.CREATE_MILESTONE, AuditEntity.MILESTONE, milestone.getMilestoneName(),
					project.getProjectName(), null, milestone, username);

		} else if(taskCreated) {

			auditService.saveAuditLog(AuditAction.CREATE_TASK, AuditEntity.TASK, task.getTaskName(),
					project.getProjectName(), null, task, username);

		} else if(subtaskCreated) {

			auditService.saveAuditLog(AuditAction.CREATE_SUBTASK, AuditEntity.SUBTASK, subtask.getSubTaskName(),
					project.getProjectName(), null, subtask, username);

		} else if(activityCreated) {

			auditService.saveAuditLog(AuditAction.CREATE_ACTIVITY, AuditEntity.ACTIVITY, activity.getActivityName(),
					project.getProjectName(), null, activity, username);
		}
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Activity created successfully", project);
	}

}