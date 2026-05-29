package com.dashboard.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.Response;
import com.dashboard.entity.Project;
import com.dashboard.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

	@Autowired
	private DashboardService dashboardService;

	@PostMapping("/upload")
	public Response uploadExcel(@RequestParam("file") MultipartFile file) {
		
		Response response = null;
		response = dashboardService.uploadExcel(file);
		return response;
	}

	@GetMapping("/projects")
	public Response getAllProjects() {

		return dashboardService.getAllProjects();
	}
}
