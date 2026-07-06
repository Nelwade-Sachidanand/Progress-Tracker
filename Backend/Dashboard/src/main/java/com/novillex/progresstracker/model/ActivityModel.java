package com.novillex.progresstracker.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ActivityModel {

	private String projectId;
	private String projectName;

	private String phaseId;
	private String phaseName;

	private String milestoneId;
	private String milestoneName;

	private String taskId;
	private String taskName;

	private String subTaskId;
	private String subTaskName;

	private String activityName;

	private String owner;

	private Double estimatedPeriodWeek;

	private LocalDate plannedStartDate;

	private LocalDate plannedEndDate;

	private LocalDate actualStartDate;

	private LocalDate actualEndDate;

	private Double actualPeriodWeek;

	private Integer progress;

	private String executionStatus;

	private String scheduleHealth;

	private String remark;
}
