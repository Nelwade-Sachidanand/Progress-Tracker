package com.novillex.progresstracker.serviceImpl;

import java.util.ArrayList;
import java.util.UUID;

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
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.CreateStructureService;
import com.novillex.progresstracker.util.UserContextUtil;
import com.novillex.progresstracker.util.WriteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CreateStructureServiceImpl implements CreateStructureService {

	private static final Logger logger = LoggerFactory.getLogger(CreateStructureServiceImpl.class);

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

		Project project = projectRepository.findById(request.getProjectId()).orElse(null);
		if (project == null) {
			logger.warn("Project not found. Project: {}", request.getProjectId());

			throw new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
					request.getProjectId());
		}

		Phase phase = null;
		if (request.isNewPhase()) {

			phase = project.getPhases().stream()
					.filter(p -> p.getPhaseName().trim().equalsIgnoreCase(request.getPhaseName().trim())).findFirst()
					.orElse(null);

			if (phase != null) {
				throw new ValidationException(ErrorCode.PHASE_ALREADY_EXISTS, "Phase already exists");
			}

			phase = new Phase();
			phase.setPhaseId(UUID.randomUUID().toString());
			phase.setPhaseName(request.getPhaseName());
			phase.setMilestones(new ArrayList<>());

			if (project.getPhases() == null) {
				project.setPhases(new ArrayList<>());
			}
			project.getPhases().add(phase);
			phaseCreated = true;

		} else {
			phase = project.getPhases().stream().filter(p -> p.getPhaseId().equals(request.getPhaseId())).findFirst()
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PHASE_NOT_FOUND, "Phase not found",
							request.getPhaseId()));
		}

		Milestone milestone = null;
		if (request.isNewMilestone()) {
			milestone = phase.getMilestones().stream()
					.filter(m -> m.getMilestoneName().trim().equalsIgnoreCase(request.getMilestoneName().trim()))
					.findFirst().orElse(null);

			if (milestone != null) {
				throw new ValidationException(ErrorCode.MILESTONE_ALREADY_EXISTS, "Milestone already exists");
			}

			milestone = new Milestone();
			milestone.setMilestoneId(UUID.randomUUID().toString());
			milestone.setMilestoneName(request.getMilestoneName());
			milestone.setTasks(new ArrayList<>());

			phase.getMilestones().add(milestone);
			milestoneCreated = true;

		} else {
			milestone = phase.getMilestones().stream().filter(m -> m.getMilestoneId().equals(request.getMilestoneId()))
					.findFirst().orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MILESTONE_NOT_FOUND,
							"Milestone not found", request.getMilestoneId()));
		}

		Task task = null;
		if (request.isNewTask()) {

			task = milestone.getTasks().stream()
					.filter(t -> t.getTaskName().trim().equalsIgnoreCase(request.getTaskName().trim())).findFirst()
					.orElse(null);

			if (task != null) {

				logger.warn("Task already exists. Task: {}, Milestone: {}", request.getTaskName(),
						milestone.getMilestoneName());

				throw new ValidationException(ErrorCode.TASK_ALREADY_EXISTS, "Task already exists");
			}

			task = new Task();
			task.setTaskId(UUID.randomUUID().toString());
			task.setTaskName(request.getTaskName());
			task.setSubTasks(new ArrayList<>());

			milestone.getTasks().add(task);

			taskCreated = true;

		} else {

			task = milestone.getTasks().stream().filter(t -> t.getTaskId().equals(request.getTaskId())).findFirst()
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TASK_NOT_FOUND, "Task not found",
							request.getTaskId()));
		}

		Subtask subtask = null;
		if (request.isNewSubTask()) {

			subtask = task.getSubTasks().stream()
					.filter(st -> st.getSubTaskName().trim().equalsIgnoreCase(request.getSubTaskName().trim()))
					.findFirst().orElse(null);

			if (subtask != null) {

				logger.warn("SubTask already exists. SubTask: {}, Task: {}", request.getSubTaskName(),
						task.getTaskName());

				throw new ValidationException(ErrorCode.SUBTASK_ALREADY_EXISTS, "SubTask already exists");
			}

			subtask = new Subtask();
			subtask.setSubTaskId(UUID.randomUUID().toString());
			subtask.setSubTaskName(request.getSubTaskName());
			subtask.setActivities(new ArrayList<>());

			task.getSubTasks().add(subtask);

			subtaskCreated = true;

		} else {

			subtask = task.getSubTasks().stream().filter(st -> st.getSubTaskId().equals(request.getSubTaskId()))
					.findFirst().orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SUBTASK_NOT_FOUND,
							"SubTask not found", request.getSubTaskId()));
		}

		Activity activity = null;
		if (request.getActivityName() != null) {

			Activity existingActivity = subtask.getActivities().stream()
					.filter(a -> a.getActivityName().trim().equalsIgnoreCase(request.getActivityName().trim()))
					.findFirst().orElse(null);

			if (existingActivity != null) {
				logger.warn("Activity already exists. Activity: {}, Project: {}", request.getActivityName(),
						project.getProjectName());

				throw new ValidationException(ErrorCode.ACTIVITY_ALREADY_EXISTS, "Activity already exists");
			}

			activity = new Activity();
			activity.setActivityId(UUID.randomUUID().toString());
			activity.setActivityName(request.getActivityName());
			activity.setOwner(request.getOwner());
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

		try {

			projectRepository.save(project);

		} catch (Exception e) {

			logger.error("Failed to save project. Project: {}", project.getProjectName(), e);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to save project");
		}
		String username = UserContextUtil.getCurrentUser();
		if (phaseCreated) {
			logger.info("New phase created. Phase: {}, Project: {}", phase.getPhaseName(), project.getProjectName());

			auditService.saveAuditLog(AuditAction.CREATE_PHASE, AuditEntity.PHASE, phase.getPhaseName(),
					project.getProjectName(), null, phase, username);

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Phase created successfully", project);

		} else if (milestoneCreated) {

			logger.info("New milestone created. Milestone: {}, Project: {}", milestone.getMilestoneName(),
					project.getProjectName());

			auditService.saveAuditLog(AuditAction.CREATE_MILESTONE, AuditEntity.MILESTONE, milestone.getMilestoneName(),
					project.getProjectName(), null, milestone, username);

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Milestone created successfully", project);

		} else if (taskCreated) {
			logger.info("New task created. Task: {}, Project: {}", task.getTaskName(), project.getProjectName());

			auditService.saveAuditLog(AuditAction.CREATE_TASK, AuditEntity.TASK, task.getTaskName(),
					project.getProjectName(), null, task, username);

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Task created successfully", project);

		} else if (subtaskCreated) {
			logger.info("New subtask created. SubTask: {}, Project: {}", subtask.getSubTaskName(),
					project.getProjectName());

			auditService.saveAuditLog(AuditAction.CREATE_SUBTASK, AuditEntity.SUBTASK, subtask.getSubTaskName(),
					project.getProjectName(), null, subtask, username);

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Subtask created successfully", project);

		} else if (activityCreated) {
			logger.info("New activity created. Activity: {}, Project: {}", activity.getActivityName(),
					project.getProjectName());

			auditService.saveAuditLog(AuditAction.CREATE_ACTIVITY, AuditEntity.ACTIVITY, activity.getActivityName(),
					project.getProjectName(), null, activity, username);
		}
		logger.info("Structure created successfully. Project: {}, User: {}", project.getProjectName(), username);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Activity created successfully", project);
	}

}