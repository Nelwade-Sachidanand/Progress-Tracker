package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class MilestoneWeightageModel {

    private String phaseId;

    private String milestoneId;

    private Double weightage;
}