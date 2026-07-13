package com.novillex.progresstracker.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadDocumentRequest {

    @NotBlank(message = "Project Id is required")
    private String projectId;

    @NotBlank(message = "Project Name is required")
    private String projectName;

    @NotBlank(message = "Bank Name is required")
    private String bankName;

    @NotBlank(message = "Phase Id is required")
    private String phaseId;

    @NotBlank(message = "Milestone Id is required")
    private String milestoneId;

    @NotBlank(message = "Task Id is required")
    private String taskId;

    @NotBlank(message = "Subtask Id is required")
    private String subTaskId;

    @NotBlank(message = "Activity Id is required")
    private String activityId;
}