package com.novillex.progresstracker.service;

import java.util.List;

import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.GenerateReportModel;

public interface ReportService {

	List<ActivityModel> generateReport(GenerateReportModel req);
}
