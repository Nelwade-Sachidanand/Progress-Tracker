package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.mapper.ActivityMapper;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.GenerateReportModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

	private ProjectRepository projectRepository;

	private ActivityMapper mapper;

	private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

	public ReportServiceImpl(ProjectRepository projectRepository, ActivityMapper mapper) {
		this.projectRepository = projectRepository;
		this.mapper = mapper;
	}

	@Override
	public List<ActivityModel> generateReport(GenerateReportModel req) {

		logger.info("Generating report for projectId: {}", req.getProjectId());

		validateReportRequest(req);

		Project project = projectRepository.findById(req.getProjectId()).orElseThrow(() -> {

			logger.warn("Project not found. ProjectId={}", req.getProjectId());

			return new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", req.getProjectId());
		});

		logger.info("Project found successfully. ProjectName={}", project.getProjectName());

		List<ActivityModel> rows = getActivities(project, req);

		if (rows.isEmpty()) {

			logger.warn(
					"No report data found. ProjectId={}, PhaseId={}, MilestoneIds={}, TaskId={}, SubTaskId={}, ActivityId={}",
					req.getProjectId(), req.getPhaseId(), req.getMilestoneIds(), req.getTaskId(), req.getSubTaskId(),
					req.getActivityId());

			throw new ResourceNotFoundException(ErrorCode.NO_REPORT_DATA_FOUND, "No records found for selected filters",
					req.getProjectId());
		}

		logger.info("Report generated successfully. Records found={}", rows.size());

		return rows;
	}

	private void validateReportRequest(GenerateReportModel model) {

		if (model.getProjectId() == null || model.getProjectId().isBlank()) {
			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select a project");
		}

		if (model.getProjectName() == null || model.getProjectName().isBlank()) {
			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Project Name is required");
		}

		boolean phaseSelected = model.getPhaseId() != null && !model.getPhaseId().isBlank();

		boolean milestoneSelected = model.getMilestoneIds() != null && !model.getMilestoneIds().isEmpty();

		boolean taskSelected = model.getTaskId() != null && !model.getTaskId().isBlank();

		boolean subTaskSelected = model.getSubTaskId() != null && !model.getSubTaskId().isBlank();

		boolean activitySelected = model.getActivityId() != null && !model.getActivityId().isBlank();

		// Phase is mandatory if anything below it is selected
		if ((milestoneSelected || taskSelected || subTaskSelected || activitySelected) && !phaseSelected) {

			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select Phase first");
		}

		// Milestone is mandatory before Task
		if (taskSelected && !milestoneSelected) {

			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select Milestone before Task");
		}

		// Task is mandatory before SubTask
		if (subTaskSelected && !taskSelected) {

			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select Task before Subtask");
		}

		// SubTask is mandatory before Activity
		if (activitySelected && !subTaskSelected) {

			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select Subtask before Activity");
		}
	}

	private List<ActivityModel> getActivities(Project project, GenerateReportModel req) {

		logger.debug("Applying filters for project: {}", project.getProjectName());

		List<ActivityModel> rows = new ArrayList<>();

		for (Phase phase : project.getPhases()) {

			if (hasText(req.getPhaseId()) && !phase.getPhaseId().equals(req.getPhaseId())) {
				continue;
			}

			for (Milestone milestone : phase.getMilestones()) {

				if (req.getMilestoneIds() != null && !req.getMilestoneIds().isEmpty()
						&& !req.getMilestoneIds().contains(milestone.getMilestoneId())) {
					continue;
				}

				for (Task task : milestone.getTasks()) {

					if (hasText(req.getTaskId()) && !task.getTaskId().equals(req.getTaskId())) {
						continue;
					}

					for (Subtask subtask : task.getSubTasks()) {

						if (hasText(req.getSubTaskId()) && !subtask.getSubTaskId().equals(req.getSubTaskId())) {
							continue;
						}

						for (Activity activity : subtask.getActivities()) {

							if (hasText(req.getActivityId()) && !activity.getActivityId().equals(req.getActivityId())) {
								continue;
							}

							if (hasText(req.getExecutionStatus())
									&& !activity.getExecutionStatus().equalsIgnoreCase(req.getExecutionStatus())) {
								continue;
							}

							if (req.getPlannedStartDate() != null && req.getPlannedEndDate() != null
									&& !isWithinDateRange(activity, req.getPlannedStartDate(),
											req.getPlannedEndDate())) {
								continue;
							}

							rows.add(mapper.toActivityModel(project, phase, milestone, task, subtask, activity));
						}
					}
				}
			}
		}

		logger.debug("Filtered activities count: {}", rows.size());

		return rows;
	}

	private boolean isWithinDateRange(Activity activity, LocalDate startDate, LocalDate endDate) {

		LocalDate activityStart = activity.getPlannedStartDate();
		LocalDate activityEnd = activity.getPlannedEndDate();

		if (activityStart == null || activityEnd == null) {
			return false;
		}

		return !activityStart.isBefore(startDate) && !activityEnd.isAfter(endDate);
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
