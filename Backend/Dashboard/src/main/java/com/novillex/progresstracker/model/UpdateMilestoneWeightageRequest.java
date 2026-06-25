package com.novillex.progresstracker.model;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMilestoneWeightageRequest {
	
	@NotBlank(message = "Project Id Required")
    private String projectId;
	
    private List<MilestoneWeightageModel> milestones;
}