package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ActivityMapper mapper;

	private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

	@Override
	public List<ActivityModel> generateReport(GenerateReportModel req) {

		logger.info("Generating report for projectId: {}", req.getProjectId());

		validateReportRequest(req);

		Project project = projectRepository.findById(req.getProjectId()).orElseThrow(() -> {
			logger.warn("Project not found: {}", req.getProjectId());

			return new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", req.getProjectId());
		});

		logger.info("Project found successfully: {}", project.getProjectName());

		List<ActivityModel> rows = getActivities(project, req);

		if (rows.isEmpty()) {

			logger.warn("No report data found. Project: {}, Phase: {}, Milestone: {}, Task: {}", req.getProjectName(),
					req.getPhaseName(), req.getMilestoneNames(), req.getTaskName());

			throw new ResourceNotFoundException(ErrorCode.NO_REPORT_DATA_FOUND, "No records found for selected filters",
					req.getProjectName());
		}

		logger.info("Report generated successfully. Records found: {}", rows.size());

		return rows;
	}

	private void validateReportRequest(GenerateReportModel model) {

		if (model.getProjectId() == null || model.getProjectId().isBlank()) {
			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select a project");
		}

		if (model.getProjectName() == null || model.getProjectName().isBlank()) {
			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Project Name is required");
		}

		boolean phaseSelected = model.getPhaseName() != null && !model.getPhaseName().isBlank();

		boolean milestoneSelected = model.getMilestoneNames() != null && !model.getMilestoneNames().isEmpty();

		boolean taskSelected = model.getTaskName() != null && !model.getTaskName().isBlank();

		boolean subTaskSelected = model.getSubtaskName() != null && !model.getSubtaskName().isBlank();

		boolean activitySelected = model.getActivityName() != null && !model.getActivityName().isBlank();

		// If anything below phase is selected, phase is mandatory
		if ((milestoneSelected || taskSelected || subTaskSelected || activitySelected) && !phaseSelected) {

			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select Phase first");
		}

		if (taskSelected && !milestoneSelected) {
			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select Milestone before Task");
		}

		if (subTaskSelected && !taskSelected) {
			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select Task before Subtask");
		}

		if (activitySelected && !subTaskSelected) {
			throw new ValidationException(ErrorCode.INVALID_REQUEST, "Please select Subtask before Activity");
		}
	}

	private List<ActivityModel> getActivities(Project project, GenerateReportModel req) {

		logger.debug("Applying filters for project: {}", project.getProjectName());

		List<ActivityModel> rows = new ArrayList<>();

		for (Phase phase : project.getPhases()) {

			if (hasText(req.getPhaseName()) && !phase.getPhaseName().equalsIgnoreCase(req.getPhaseName())) {
				continue;
			}

			for (Milestone milestone : phase.getMilestones()) {

				if (req.getMilestoneNames() != null && !req.getMilestoneNames().isEmpty() && req.getMilestoneNames()
						.stream().noneMatch(m -> m.equalsIgnoreCase(milestone.getMilestoneName()))) {
					continue;
				}

				for (Task task : milestone.getTasks()) {

					if (hasText(req.getTaskName()) && !task.getTaskName().equalsIgnoreCase(req.getTaskName())) {
						continue;
					}

					for (Subtask subtask : task.getSubTasks()) {

						if (hasText(req.getSubtaskName())
								&& !subtask.getSubTaskName().equalsIgnoreCase(req.getSubtaskName())) {
							continue;
						}

						for (Activity activity : subtask.getActivities()) {

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
