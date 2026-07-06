package com.novillex.progresstracker.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Activity {
	
	private String activityId;
	
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
    
    private Boolean locked = false;

    private String lockedBy;

    private LocalDateTime lockedAt;
}
