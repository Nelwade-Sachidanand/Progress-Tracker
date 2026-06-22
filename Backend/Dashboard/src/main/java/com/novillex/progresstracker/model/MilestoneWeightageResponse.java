package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class MilestoneWeightageResponse {

	private String phaseName;

	private String milestoneName;

	private Double weightage;
}