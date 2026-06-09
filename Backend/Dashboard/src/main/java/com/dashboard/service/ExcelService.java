
package com.dashboard.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.Response;
import com.dashboard.model.ActivityModel;

public interface ExcelService {
	byte[] generateExcel(List<ActivityModel> reportRequest);
    Response uploadExcel(MultipartFile file);
}
