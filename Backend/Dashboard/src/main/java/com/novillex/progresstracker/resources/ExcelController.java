package com.novillex.progresstracker.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.service.ExcelService;

@RestController
@RequestMapping("/excel")
@PreAuthorize("hasAnyRole('ADMIN','IMPLEMENTATION USER')")
public class ExcelController {

	private static final Logger logger = LoggerFactory.getLogger(ExcelController.class);

	
	private ExcelService excelService;
	
	public ExcelController(ExcelService excelService) {
		this.excelService=excelService;
	}

	@PostMapping("/upload")
	public Response uploadExcel(@RequestParam("file") MultipartFile file) {
		logger.info("Excel upload request received. File: {}", file.getOriginalFilename());

		Response response = null;
		response = excelService.uploadExcel(file);
		return response;
	}
}
