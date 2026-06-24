package com.novillex.progresstracker.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateReportModel {
	@NotBlank(message = "Please select a project")
	private String projectId;
	
	@NotBlank(message = "Project Name is required")
	private String projectName;
	
	private String phaseName;
	List<String> milestoneNames;
	private String taskName;
	private String subtaskName;
	private String activityName;
	private String executionStatus;
	private LocalDate plannedStartDate;
	private LocalDate plannedEndDate;
}
