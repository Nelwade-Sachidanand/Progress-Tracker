package com.dashboard.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

	@Override
	public List<ActivityModel> generateReport(GenerateReportModel req) {

		Project project = projectRepository.findByProjectName(req.getProjectName())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
						req.getProjectName()));

		List<ActivityModel> rows = getActivities(project, req);

		if (rows.isEmpty()) {
			throw new ResourceNotFoundException(ErrorCode.NO_REPORT_DATA_FOUND, "No records found for selected filters",
					req.getProjectName());
		}

		return rows;
	}

	private List<ActivityModel> getActivities(Project project, GenerateReportModel req) {

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
