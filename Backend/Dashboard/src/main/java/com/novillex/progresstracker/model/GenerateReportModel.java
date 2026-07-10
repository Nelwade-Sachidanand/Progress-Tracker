package com.novillex.progresstracker.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateReportModel {

    @NotBlank(message = "Please select a project")
    private String projectId;

    @NotBlank(message = "Project Name is required")
    private String projectName;

    private String phaseId;

    private List<String> milestoneIds;

    private String taskId;

    private String subTaskId;

    private String activityId;

    private String executionStatus;

    private LocalDate plannedStartDate;

    private LocalDate plannedEndDate;
}