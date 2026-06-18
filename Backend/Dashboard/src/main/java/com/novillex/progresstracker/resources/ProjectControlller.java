package com.novillex.progresstracker.resources;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ProjectIdsRequest;
import com.novillex.progresstracker.service.ProjectService;

@RestController
@RequestMapping("/projects")
public class ProjectControlller {

	@Autowired
	private ProjectService projectService;

	@GetMapping("/getAll")
	public Response getAllProjects() {

		return projectService.getAllProjects();
	}

	@DeleteMapping("/delete/{projectId}")
	public Response deleteProject(@PathVariable String projectId) {

		return projectService.deleteProject(projectId);
	}

	@PostMapping("/getNames")
	public Response getProjectNames(@RequestBody ProjectIdsRequest request) {

		return projectService.getProjectNames(request.getProjectIds());
	}
}
