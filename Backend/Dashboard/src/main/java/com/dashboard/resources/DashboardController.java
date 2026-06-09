package com.dashboard.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.Response;
import com.dashboard.model.ActivityModel;
import com.dashboard.model.GenerateReportModel;
import com.dashboard.service.DashboardService;
import com.dashboard.service.ExcelService;
import com.dashboard.service.ReportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
	
	private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
	@Autowired
	private DashboardService dashboardService;
	
	@Autowired
	private ExcelService excelService;
	
	@Autowired
	private ReportService reportService;

	@PostMapping("/upload/excel")
	public Response uploadExcel(@RequestParam("file") MultipartFile file) {
		logger.info("Excel upload request received. File: {}", file.getOriginalFilename());

		Response response = null;
		response = excelService.uploadExcel(file);
		return response;
	}

	@GetMapping("/projects")
	public Response getAllProjects() {
		return dashboardService.getAllProjects();
	}

	@GetMapping("/generate/report")
	public ResponseEntity<byte[]> export(@RequestBody GenerateReportModel request) {

		List<ActivityModel> reports = reportService.generateReport(request);

		byte[] excel = excelService.generateExcel(reports);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.xlsx")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(excel);
	}
}
