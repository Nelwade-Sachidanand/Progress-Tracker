package com.dashboard.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.common.Response;
import com.dashboard.model.ActivityModel;
import com.dashboard.service.CreateStructureService;
import com.dashboard.service.UpdateActivityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin
public class ActivityController {

	private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

	@Autowired
	private UpdateActivityService updateActivityService;

	@Autowired
	private CreateStructureService createStructureService;

	@PutMapping("/update/activity")
	public Response updateActivity(@RequestBody ActivityModel updateActivity) {

		logger.info("Update Activity request received for project: {}, activity: {}",
				updateActivity.getProjectName(),
				updateActivity.getActivityName());

		return updateActivityService.updateActivity(updateActivity);
	}

	@PostMapping("/create/activity")
	public Response createStructure(@RequestBody ActivityModel request) {

		logger.info("Create Activity request received for project: {}, activity: {}",
				request.getProjectName(),
				request.getActivityName());

		return createStructureService.createStructure(request);
	}
}