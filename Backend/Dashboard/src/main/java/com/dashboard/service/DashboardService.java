package com.dashboard.service;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.Response;
import com.dashboard.entity.Project;

public interface DashboardService {

	Response uploadExcel(MultipartFile file);
	Response getAllProjects();
	ByteArrayInputStream exportExcel(String projectName);
}

