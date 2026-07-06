package com.novillex.progresstracker.model;

import java.time.LocalDate;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActivityUpdateRequestModel {
	
	@NotBlank(message = "Project Id required")
	private String projectId;
	
	@NotBlank(message = "Project Name can not blank")
	private String projectName;
	
	@NotBlank(message = "Phase Id required")
	private String phaseId;
	
	@NotBlank(message = "Milestone Id required")
	private String milestoneId;
	
	@NotBlank(message = "Task Id required")
	private String taskId;
	
	@NotBlank(message = "Subtask Id required")
	private String subTaskId;
	
	@NotBlank(message = "Phase Name can not blank")
	private String phaseName;

	@NotBlank(message = "Milestone can not blank")
	private String milestoneName;
	
	@NotBlank(message = "Task Name can not blank")
	private String taskName;
	
	@NotBlank(message = "Subtask can not blank")
	private String subTaskName;
	
	@NotBlank(message = "Activity id can not blank")
	private String activityId;

	@NotBlank(message = "Activity can not blank")
	private String activityName;

	private String owner;
	
	@NotNull(message = "Estimated Period can not be null")
	private Double estimatedPeriodWeek;

	private LocalDate plannedStartDate;

	private LocalDate plannedEndDate;

	private LocalDate actualStartDate;

	private LocalDate actualEndDate;

	private Double actualPeriodWeek;

	private Integer progress;

	private String executionStatus;

	private String scheduleHealth;

	private String changeReason;
}
