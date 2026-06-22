package com.novillex.progresstracker.model;

import java.util.List;

import lombok.Data;

@Data
public class UpdateMilestoneWeightageRequest {

    private String projectId;

    private String phaseName;

    private List<MilestoneWeightageModel> milestones;
}