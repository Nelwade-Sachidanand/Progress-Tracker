package com.dashboard.service;

import java.util.List;

import com.dashboard.model.ActivityModel;
import com.dashboard.model.GenerateReportModel;

public interface ReportService {

	List<ActivityModel> generateReport(GenerateReportModel req);
}
