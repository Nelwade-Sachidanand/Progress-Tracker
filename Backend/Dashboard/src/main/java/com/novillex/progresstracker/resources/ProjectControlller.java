package com.novillex.progresstracker.resources;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ProjectIdsRequest;
import com.novillex.progresstracker.model.UpdateMilestoneWeightageRequest;
import com.novillex.progresstracker.service.ProjectService;

@RestController
@RequestMapping("/projects")
public class ProjectControlller {

	private static final Logger logger = LoggerFactory.getLogger(ProjectControlller.class);

	@Autowired
	private ProjectService projectService;

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/getAll")
	public Response getAllProjects() {

		logger.info("Get all projects request received");

		Response response = projectService.getAllProjects();

		logger.info("Get all projects request completed");

		return response;
	}

	@GetMapping("/user/{userId}/projects")
	public Response getProjectsByUserId(@PathVariable String userId) {

		logger.info("Get projects by user request received. UserId={}", userId);

		Response response = projectService.getProjectsByUserId(userId);

		logger.info("Get projects by user request completed. UserId={}", userId);

		return response;
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/delete/{projectId}")
	public Response deleteProject(@PathVariable String projectId) {

		logger.info("Delete project request received. ProjectId={}", projectId);

		Response response = projectService.deleteProject(projectId);

		logger.info("Delete project request completed. ProjectId={}", projectId);

		return response;
	}

	@PostMapping("/getNames")
	public Response getProjectNames(@RequestBody ProjectIdsRequest request) {

		logger.info("Get project names request received. ProjectIds={}", request.getProjectIds());

		Response response = projectService.getProjectNames(request.getProjectIds());

		logger.info("Get project names request completed");

		return response;
	}
	
	@PreAuthorize("hasAnyRole('ADMIN','IMPLEMENTATION USER')")
	@PutMapping("/milestone-weightages")
	public Response updateMilestoneWeightages(@RequestBody UpdateMilestoneWeightageRequest request) {

		logger.info("Update milestone weightages request received. ProjectId={}", request.getProjectId());

		Response response = projectService.updateMilestoneWeightages(request);

		logger.info("Update milestone weightages request completed. ProjectId={}", request.getProjectId());

		return response;
	}
	
	@PreAuthorize("hasAnyRole('ADMIN','IMPLEMENTATION USER')")
	@GetMapping("/milestone-weightages/{projectId}")
	public Response getMilestoneWeightages(@PathVariable String projectId) {

		logger.info("Get milestone weightages request received. ProjectId={}", projectId);

		Response response = projectService.getMilestoneWeightages(projectId);

		logger.info("Get milestone weightages request completed. ProjectId={}", projectId);

		return response;
	}
}