package com.novillex.progresstracker.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddRemarkModel {
	
	@NotBlank(message = "Project Id Required")
	private String projectId;
	
	@NotBlank(message = "Project Name Required")
	private String projectName;
	
	@NotBlank(message = "Phase Name Required")
	private String phaseName;
	
	@NotBlank(message = "Milestone Name Required")
	private String milestoneName;
	
	@NotBlank(message = "Task Name Required")
	private String taskName;
	
	@NotBlank(message = "Subtask Name Required")
	private String subTaskName;
	
	@NotBlank(message = "Activity Name Required")
	private String activityName;
	
	@NotBlank(message = "Remark Required")
	private String remark;
}
