package com.dashboard.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.util.ExcelParserUtil;
import com.dashboard.entity.Activity;
import com.dashboard.entity.Milestone;
import com.dashboard.entity.Phase;
import com.dashboard.entity.Project;
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.model.ExcelRowModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

	@Autowired
	private ProjectRepository projectRepository;

	@Override
	public String uploadExcel(MultipartFile file) {

		try {

			List<ExcelRowModel> rows = ExcelParserUtil.parseExcel(file);

			for (ExcelRowModel model : rows) {

				Optional<Project> optionalProject = projectRepository.findByProjectName(model.getProjectName());

				Project project = optionalProject.orElseGet(Project::new);

				project.setProjectName(model.getProjectName());

				if (project.getPhases() == null) {

					project.setPhases(new ArrayList<>());
				}

				Phase phase = project.getPhases().stream().filter(p -> p.getPhaseName().equals(model.getPhaseName()))
						.findFirst().orElse(null);

				if (phase == null) {

					phase = new Phase();

					phase.setPhaseName(model.getPhaseName());

					phase.setMilestones(new ArrayList<>());

					project.getPhases().add(phase);
				}

				Milestone milestone = phase.getMilestones().stream()
						.filter(m -> m.getMilestoneName().equals(model.getMilestoneName())).findFirst().orElse(null);

				if (milestone == null) {

					milestone = new Milestone();

					milestone.setMilestoneName(model.getMilestoneName());

					milestone.setTasks(new ArrayList<>());

					phase.getMilestones().add(milestone);
				}

				Task task = milestone.getTasks().stream().filter(t -> t.getTaskName().equals(model.getTaskName()))
						.findFirst().orElse(null);

				if (task == null) {

					task = new Task();

					task.setTaskName(model.getTaskName());

					task.setSubTasks(new ArrayList<>());

					milestone.getTasks().add(task);
				}

				Subtask subTask = task.getSubTasks().stream()
						.filter(st -> st.getSubTaskName().equals(model.getSubTaskName())).findFirst().orElse(null);

				if (subTask == null) {

					subTask = new Subtask();

					subTask.setSubTaskName(model.getSubTaskName());

					subTask.setActivities(new ArrayList<>());

					task.getSubTasks().add(subTask);
				}

				Activity activity = subTask.getActivities().stream()
						.filter(a -> a.getActivityName().equals(model.getActivityName())).findFirst().orElse(null);

				if (activity == null) {

					activity = new Activity();

					activity.setActivityName(model.getActivityName());

					subTask.getActivities().add(activity);
				}

				activity.setEstimatedPeriodWeek(model.getEstimatedPeriodWeek());

				activity.setPlannedStartDate(model.getPlannedStartDate());

				activity.setPlannedEndDate(model.getPlannedEndDate());

				activity.setActualStartDate(model.getActualStartDate());

				activity.setActualEndDate(model.getActualEndDate());

				activity.setActualPeriodWeek(model.getActualPeriodWeek());

				activity.setProgress(model.getProgress());

				activity.setExecutionStatus(model.getExecutionStatus());

				activity.setScheduleHealth(model.getScheduleHealth());

				projectRepository.save(project);
			}

			return "Excel Uploaded Successfully";

		} catch (Exception e) {

			e.printStackTrace();

			return "Failed To Upload Excel";
		}
	}
	
	@Override
	public List<Project> getAllProjects() {

	    return projectRepository.findAll();
	}
}
