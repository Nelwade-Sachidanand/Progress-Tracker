package com.novillex.progresstracker.mapper;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.model.ActivityModel;

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
