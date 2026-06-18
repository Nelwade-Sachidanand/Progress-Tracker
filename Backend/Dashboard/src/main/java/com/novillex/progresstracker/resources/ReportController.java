package com.novillex.progresstracker.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
	
	@Autowired
	private ReportService reportService;
	
	@Autowired
	private ExcelService excelService;

	@PostMapping("/generate/report")
	public ResponseEntity<byte[]> export(@RequestBody GenerateReportModel request) {

		List<ActivityModel> reports = reportService.generateReport(request);
				System.out.println(reports);
		byte[] excel = excelService.generateExcel(reports);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.xlsx")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(excel);
	}
}
