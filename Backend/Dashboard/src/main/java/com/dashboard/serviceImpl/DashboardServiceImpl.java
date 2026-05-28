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
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.model.ExcelRowModel;
import com.dashboard.repository.PhaseRepository;
import com.dashboard.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

	@Autowired
	private PhaseRepository phaseRepository;

	@Override
	public String uploadExcel(MultipartFile file) {

		try {

			List<ExcelRowModel> rows = ExcelParserUtil.parseExcel(file);

			for (ExcelRowModel model : rows) {

				Optional<Phase> optionalPhase = phaseRepository.findByPhaseName(model.getPhaseName());

				Phase phase = optionalPhase.orElseGet(Phase::new);

				phase.setPhaseName(model.getPhaseName());

				if (phase.getMilestones() == null) {
					phase.setMilestones(new ArrayList<>());
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

				activity.setProgress(model.getProgress());

				activity.setExecutionStatus(model.getExecutionStatus());

				activity.setScheduleHealth(model.getScheduleHealth());

				phaseRepository.save(phase);
			}

			return "Excel Uploaded Successfully";

		} catch (Exception e) {

			e.printStackTrace();

			return "Failed To Upload Excel";
		}
	}
}
