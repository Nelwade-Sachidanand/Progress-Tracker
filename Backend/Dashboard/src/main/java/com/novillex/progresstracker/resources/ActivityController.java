package com.novillex.progresstracker.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.ActivityUpdateRequestModel;
import com.novillex.progresstracker.model.AddRemarkModel;
import com.novillex.progresstracker.service.CreateStructureService;
import com.novillex.progresstracker.service.UpdateActivityService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/activity")
public class ActivityController {

	private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

	@Autowired
	private UpdateActivityService updateActivityService;

	@Autowired
	private CreateStructureService createStructureService;

	@PreAuthorize("hasAnyRole('ADMIN','IMPLEMENTATION USER')")
	@PutMapping("/update/request")
	public Response updateActivityRequest(@Valid @RequestBody ActivityUpdateRequestModel request) {

		logger.info("Update Activity Request received for Activity: {}", request.getActivityName());
		return updateActivityService.updateActivityRequest(request);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN','IMPLEMENTATION USER')")
	@PostMapping("/create")
	public Response createStructure(@RequestBody ActivityModel request) {

		logger.info("Create Activity request received for project: {}, activity: {}", request.getProjectName(),
				request.getActivityName());

		return createStructureService.createStructure(request);
	}

	@PostMapping("/add/remark")
	public Response addRemark(@Valid @RequestBody AddRemarkModel addRemarkModel) {

		logger.info("Add Remark request received for activity : {},", addRemarkModel.getActivityName());

		return updateActivityService.addRemark(addRemarkModel);
	}
}