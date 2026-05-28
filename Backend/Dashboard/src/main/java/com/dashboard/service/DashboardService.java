package com.dashboard.service;

import org.springframework.web.multipart.MultipartFile;

public interface DashboardService {

    String uploadExcel(MultipartFile file);
}

