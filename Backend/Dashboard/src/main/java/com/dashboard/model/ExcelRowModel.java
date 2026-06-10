package com.dashboard.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExcelRowModel {

	private String bankName;

	private String projectManager;

	private String projectName;

	private String phaseName;

	private String milestoneName;

	private String taskName;

	private String subTaskName;

	private String activityName;

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
