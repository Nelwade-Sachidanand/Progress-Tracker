package com.dashboard.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Activity;
import com.dashboard.entity.Milestone;
import com.dashboard.entity.Phase;
import com.dashboard.entity.Project;
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.model.ActivityModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.UpdateActivityService;

@Service
public class UpdateActivityServiceImpl implements UpdateActivityService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ApplicationContext context;

	@Override
	public Response updateActivity(ActivityModel request) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		Project project = projectRepository.findByProjectName(request.getProjectName()).orElse(null);

		if (project == null) {

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE, "Project not found",
					null);
		}

		Activity activityToUpdate = null;

		for (Phase phase : project.getPhases()) {

			if (!phase.getPhaseName().equals(request.getPhaseName())) {
				continue;
			}

			for (Milestone milestone : phase.getMilestones()) {

				if (!milestone.getMilestoneName().equals(request.getMilestoneName())) {
					continue;
				}

				for (Task task : milestone.getTasks()) {

					if (!task.getTaskName().equals(request.getTaskName())) {
						continue;
					}

					for (Subtask subTask : task.getSubTasks()) {

						if (!subTask.getSubTaskName().equals(request.getSubTaskName())) {
							continue;
						}

						for (Activity activity : subTask.getActivities()) {

							if (activity.getActivityName().equals(request.getActivityName())) {

								activityToUpdate = activity;
								break;
							}
						}
					}
				}
			}
		}

		if (activityToUpdate == null) {

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE, "Activity not found",
					null);
		}

		activityToUpdate.setEstimatedPeriodWeek(request.getEstimatedPeriodWeek());

		activityToUpdate.setPlannedStartDate(request.getPlannedStartDate());

		activityToUpdate.setPlannedEndDate(request.getPlannedEndDate());

		activityToUpdate.setActualStartDate(request.getActualStartDate());

		activityToUpdate.setActualEndDate(request.getActualEndDate());

		activityToUpdate.setProgress(request.getProgress());
		
		

		projectRepository.save(project);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Activity updated successfully", activityToUpdate);
	}

}
