package com.novillex.progresstracker.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ProjectInformationModel;
import com.novillex.progresstracker.service.ProjectInformationService;

@RestController
@RequestMapping("/project-information")
public class ProjectInformationController {

	@Autowired
	private ProjectInformationService projectInformationService;

	@PostMapping("/create")
	public Response createProjectInformation(@RequestBody ProjectInformationModel model) {

		return projectInformationService.createProjectInformation(model);
	}

	@GetMapping("/all")
	public Response getAllProjectInformation() {

		return projectInformationService.getAllProjectInformation();
	}

	@GetMapping("/{id}")
	public Response getProjectInformationById(@PathVariable String id) {

		return projectInformationService.getProjectInformationById(id);
	}

	@PutMapping("/update/{id}")
	public Response updateProjectInformation(@PathVariable String id, @RequestBody ProjectInformationModel model) {

		return projectInformationService.updateProjectInformation(id, model);
	}

	@DeleteMapping("/delete/{id}")
	public Response deleteProjectInformation(@PathVariable String id) {

		return projectInformationService.deleteProjectInformation(id);
	}
}