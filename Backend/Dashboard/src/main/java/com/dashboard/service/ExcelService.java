
package com.dashboard.service;

import java.util.List;

import com.dashboard.model.ActivityModel;

public interface ExcelService {
	byte[] generateExcel(List<ActivityModel> reportRequest);
}
