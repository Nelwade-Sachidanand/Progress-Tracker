package com.novillex.progresstracker.util;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.HierarchyReference;

public class HierarchyReferenceUtil {

	public static HierarchyReference findHierarchy(Project project, String activityId) {

		HierarchyReference ref = new HierarchyReference();

		for (Phase phase : project.getPhases()) {

			for (Milestone milestone : phase.getMilestones()) {

				for (Task task : milestone.getTasks()) {

					for (Subtask subTask : task.getSubTasks()) {

						if (subTask.getActivities() == null) {
							continue;
						}

						for (Activity activity : subTask.getActivities()) {

							if (activityId.equals(activity.getActivityId())) {

								ref.setPhase(phase);
								ref.setMilestone(milestone);
								ref.setTask(task);
								ref.setSubTask(subTask);
								ref.setActivity(activity);

								return ref;
							}
						}
					}
				}
			}
		}

		throw new ResourceNotFoundException(ErrorCode.ACTIVITY_NOT_FOUND, "Activity not found", activityId);
	}
}
