package com.dashboard.model;

import lombok.Data;

@Data
public class GenerateReportModel {
	private String projectName;
	private String phaseName;
	private String milestoneName;
	private String taskName;
	private String subtaskName;
	private String activityName;
	private String executionStatus;
}
