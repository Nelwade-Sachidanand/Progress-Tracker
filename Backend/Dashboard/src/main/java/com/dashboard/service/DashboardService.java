package com.dashboard.service;

import java.io.ByteArrayInputStream;

import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.Response;

public interface DashboardService {

	Response uploadExcel(MultipartFile file);
	
	Response getAllProjects();
	
	ByteArrayInputStream exportExcel(String projectName);
}

