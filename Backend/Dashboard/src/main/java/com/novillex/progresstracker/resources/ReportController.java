package com.novillex.progresstracker.resources;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.GenerateReportModel;
import com.novillex.progresstracker.service.ExcelService;
import com.novillex.progresstracker.service.ReportService;

@RestController
@RequestMapping("/reports")
public class ReportController {
	
	
	private ReportService reportService;
	
	
	private ExcelService excelService;
	
	public ReportController(ReportService reportService, ExcelService excelService) {
		this.reportService=reportService;
		this.excelService=excelService;
	}

	@PostMapping("/generate/report")
	public ResponseEntity<byte[]> export(@RequestBody GenerateReportModel request) {

		List<ActivityModel> reports = reportService.generateReport(request);
		byte[] excel = excelService.generateExcel(reports);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.xlsx")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(excel);
	}
}
