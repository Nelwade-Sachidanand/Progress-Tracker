package com.novillex.progresstracker.resources;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ProjectInformationModel;
import com.novillex.progresstracker.service.ProjectInformationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/project-information")
@PreAuthorize("hasRole('ADMIN')")
public class ProjectInformationController {

	
	private ProjectInformationService projectInformationService;
	
	public ProjectInformationController(ProjectInformationService projectInformationService) {
		this.projectInformationService=projectInformationService;
	}

	@PostMapping("/create")
	public Response createProjectInformation(@Valid @RequestBody ProjectInformationModel model) {
		
		return projectInformationService.createProjectInformation(model);
	}

	@GetMapping("/all")
	public Response getAllProjectInformation() {

		return projectInformationService.getAllProjectInformation();
	}

	@GetMapping("/getProjectInformation")
	public Response getProjectInformation(@RequestParam String bankName, @RequestParam String projectName) {

		return projectInformationService.getProjectInformation(bankName, projectName);
	}

	@GetMapping("/{id}")
	public Response getProjectInformationById(@PathVariable String id) {

		return projectInformationService.getProjectInformationById(id);
	}

	@PutMapping("/update/{id}")
	public Response updateProjectInformation(@RequestBody ProjectInformationModel model) {
		System.out.println(model);
		return projectInformationService.updateProjectInformation( model);
	}

	@DeleteMapping("/delete/{id}")
	public Response deleteProjectInformation(@PathVariable String id) {

		return projectInformationService.deleteProjectInformation(id);
	}
}