package com.dashboard.resources;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.Response;
import com.dashboard.entity.Project;
import com.dashboard.model.ActivityModel;
import com.dashboard.model.GenerateReportModel;
import com.dashboard.service.DashboardService;
import com.dashboard.service.ExcelService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class DashboardController {
	private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
	@Autowired
	private DashboardService dashboardService;
	
	@Autowired
	private ExcelService excelService;

	@PostMapping("/upload/excel")
	public Response uploadExcel(@RequestParam("file") MultipartFile file) {
		logger.info("Excel upload request received. File: {}", file.getOriginalFilename());
		// System.out.println(file.getOriginalFilename());
		Response response = null;
		response = dashboardService.uploadExcel(file);
		return response;
	}

	@GetMapping("/projects")
	public Response getAllProjects() {
		return dashboardService.getAllProjects();
	}

	@GetMapping("/export/{projectName}")
	public ResponseEntity<InputStreamResource> exportExcel(@PathVariable String projectName) {
		logger.info("Excel export request received. Project: {}", projectName);
		ByteArrayInputStream stream = dashboardService.exportExcel(projectName);
		System.out.println(projectName);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + projectName + ".xlsx")
				.contentType(
						MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(new InputStreamResource(stream));
	}

	@GetMapping("/generate/report")
	public ResponseEntity<byte[]> export(@RequestBody GenerateReportModel request) {

		List<ActivityModel> reports = dashboardService.generateReport(request);

		byte[] excel = excelService.generateExcel(reports);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.xlsx")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(excel);
	}
}
