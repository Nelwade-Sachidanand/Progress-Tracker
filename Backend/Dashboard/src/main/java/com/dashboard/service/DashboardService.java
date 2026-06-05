package com.dashboard.service;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.Response;
import com.dashboard.model.ActivityModel;
import com.dashboard.model.GenerateReportModel;

public interface DashboardService {

	Response uploadExcel(MultipartFile file);
	
	Response getAllProjects();
	
	ByteArrayInputStream exportExcel(String projectName);
	
	List<ActivityModel> generateReport(GenerateReportModel req);
}

