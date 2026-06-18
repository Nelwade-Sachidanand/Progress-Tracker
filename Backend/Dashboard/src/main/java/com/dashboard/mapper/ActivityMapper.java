package com.dashboard.mapper;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.dashboard.entity.Activity;
import com.dashboard.entity.Milestone;
import com.dashboard.entity.Phase;
import com.dashboard.entity.Project;
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.model.ActivityModel;

@Service
public class ActivityMapper {

	public ActivityModel toActivityModel(Project project, Phase phase, Milestone milestone, Task task, Subtask subtask,
			Activity activity) {

		ActivityModel row = new ActivityModel();

		BeanUtils.copyProperties(activity, row);
		row.setProjectId(project.getId());
		row.setProjectName(project.getProjectName());
		row.setPhaseName(phase.getPhaseName());
		row.setMilestoneName(milestone.getMilestoneName());
		row.setTaskName(task.getTaskName());
		row.setSubTaskName(subtask.getSubTaskName());

		return row;
	}
}
