package com.dashboard.entity;

import java.time.LocalDate;

import lombok.Data;

@Data
public class Activity {
	
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
}
