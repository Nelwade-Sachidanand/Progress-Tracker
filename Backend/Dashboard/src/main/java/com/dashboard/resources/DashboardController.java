package com.dashboard.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dashboard.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
                dashboardService.uploadExcel(file));
    }
}

