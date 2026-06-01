package com.dashboard.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class Activity {
	
	private String projectName;
	
	private String phaseName;
	
	private String milestoneName;
	
	private String taskName;

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
}
