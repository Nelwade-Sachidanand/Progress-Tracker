package com.novillex.progresstracker.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class GenerateReportModel {
	private String projectId;
	private String projectName;
	private String phaseName;
	private String milestoneName;
	private String taskName;
	private String subtaskName;
	private String activityName;
	private String executionStatus;
	private LocalDate plannedStartDate;
	private LocalDate plannedEndDate;
}
