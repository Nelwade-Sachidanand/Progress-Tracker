
package com.novillex.progresstracker.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ActivityModel;

public interface ExcelService {
	byte[] generateExcel(List<ActivityModel> reportRequest);
    Response uploadExcel(MultipartFile file);
}
