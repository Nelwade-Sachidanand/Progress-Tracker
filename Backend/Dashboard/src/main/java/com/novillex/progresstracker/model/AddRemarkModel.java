package com.novillex.progresstracker.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddRemarkModel {
	
	@NotBlank(message = "Project Id Required")
	private String projectId;
	
	@NotBlank(message = "Project Name Required")
	private String projectName;
	
	@NotBlank(message = "Phase Id Required")
	private String phaseId;
	
	@NotBlank(message = "Milestone Id Required")
	private String milestoneId;
	
	@NotBlank(message = "Task Id Required")
	private String taskId;
	
	@NotBlank(message = "Subtask Id Required")
	private String subTaskId;
	
	@NotBlank(message = "Activity Id Required")
	private String activityId;
	
	@NotBlank(message = "Remark Required")
	private String remark;
}
