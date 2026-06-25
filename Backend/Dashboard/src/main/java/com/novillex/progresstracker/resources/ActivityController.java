package com.novillex.progresstracker.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.ActivityUpdateRequestModel;
import com.novillex.progresstracker.service.CreateStructureService;
import com.novillex.progresstracker.service.UpdateActivityService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/dashboard")
public class ActivityController {

	private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

	@Autowired
	private UpdateActivityService updateActivityService;

	@Autowired
	private CreateStructureService createStructureService;
	
	
	@PutMapping("/update/activity/request")
	public Response updateActivityRequest(@Valid @RequestBody ActivityUpdateRequestModel request) {
		
		logger.info("Update Activity Request received for Activity: {}", request.getActivityName());
		return updateActivityService.updateActivityRequest(request);
	}
	

//	@PutMapping("/update/activity")
//	public Response updateActivity(@RequestBody ActivityModel updateActivity) {
//
//		logger.info("Update Activity request received for project: {}, activity: {}",
//				updateActivity.getProjectName(),
//				updateActivity.getActivityName());
//
//		return updateActivityService.updateActivity(updateActivity);
//	}

	@PostMapping("/create/activity")
	public Response createStructure(@RequestBody ActivityModel request) {

		logger.info("Create Activity request received for project: {}, activity: {}",
				request.getProjectName(),
				request.getActivityName());

		return createStructureService.createStructure(request);
	}
}