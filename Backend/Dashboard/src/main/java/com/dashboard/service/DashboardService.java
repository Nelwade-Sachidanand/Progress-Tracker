package com.dashboard.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.dashboard.model.ExcelRowModel;

public interface DashboardService {

	String uploadExcel(MultipartFile file);
}

