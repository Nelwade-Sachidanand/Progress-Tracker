package com.novillex.progresstracker.service;

import org.springframework.web.multipart.MultipartFile;

public interface VirusScanService {

    void scan(MultipartFile file);

}