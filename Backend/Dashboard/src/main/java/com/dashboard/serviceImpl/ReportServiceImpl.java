package com.dashboard.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dashboard.common.ErrorCode;
import com.dashboard.entity.Activity;
import com.dashboard.entity.Milestone;
import com.dashboard.entity.Phase;
import com.dashboard.entity.Project;
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.exception.ResourceNotFoundException;
import com.dashboard.mapper.ActivityMapper;
import com.dashboard.model.ActivityModel;
import com.dashboard.model.GenerateReportModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ActivityMapper mapper;

	private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

	@Override
	public List<ActivityModel> generateReport(GenerateReportModel req) {

		logger.info("Generating report for project: {}", req.getProjectName());

		Project project = projectRepository.findByProjectName(req.getProjectName()).orElseThrow(() -> {
			logger.warn("Project not found: {}", req.getProjectName());

			return new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
					req.getProjectName());
		});

		logger.info("Project found successfully: {}", project.getProjectName());

		List<ActivityModel> rows = getActivities(project, req);

		if (rows.isEmpty()) {

			logger.warn("No report data found. Project: {}, Phase: {}, Milestone: {}, Task: {}", req.getProjectName(),
					req.getPhaseName(), req.getMilestoneName(), req.getTaskName());

			throw new ResourceNotFoundException(ErrorCode.NO_REPORT_DATA_FOUND, "No records found for selected filters",
					req.getProjectName());
		}

		logger.info("Report generated successfully. Records found: {}", rows.size());

		return rows;
	}

	private List<ActivityModel> getActivities(Project project, GenerateReportModel req) {

		logger.debug("Applying filters for project: {}", project.getProjectName());

		List<ActivityModel> rows = new ArrayList<>();

		for (Phase phase : project.getPhases()) {

			if (hasText(req.getPhaseName()) && !phase.getPhaseName().equalsIgnoreCase(req.getPhaseName())) {
				continue;
			}

			for (Milestone milestone : phase.getMilestones()) {

				if (hasText(req.getMilestoneName())
						&& !milestone.getMilestoneName().equalsIgnoreCase(req.getMilestoneName())) {
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
