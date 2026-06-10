package com.dashboard.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.model.ActivityModel;
import com.dashboard.model.GenerateReportModel;
import com.dashboard.service.ExcelService;
import com.dashboard.service.ReportService;

@RestController
@RequestMapping("/reports")
public class ReportController {
	
	@Autowired
	private ReportService reportService;
	
	@Autowired
	private ExcelService excelService;

	@GetMapping("/generate/report")
	public ResponseEntity<byte[]> export(@RequestBody GenerateReportModel request) {

		List<ActivityModel> reports = reportService.generateReport(request);

		byte[] excel = excelService.generateExcel(reports);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.xlsx")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(excel);
	}
}
