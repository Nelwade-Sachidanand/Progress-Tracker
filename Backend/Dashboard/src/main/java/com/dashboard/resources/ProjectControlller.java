package com.dashboard.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.common.Response;
import com.dashboard.service.ProjectService;

@RestController
@RequestMapping("/projects")
public class ProjectControlller {
	
	@Autowired
	private ProjectService projectService;
	
	@GetMapping("/getAll")
	public Response getAllProjects() {
		
		return projectService.getAllProjects();
	}

	@DeleteMapping("/delete/{projectName}")
	public Response deleteProject(@PathVariable String projectName) {

		return projectService.deleteProject(projectName);
	}
}
