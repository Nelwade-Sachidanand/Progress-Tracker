package com.dashboard.entity;

import java.util.List;


import lombok.Data;

@Data
public class Phase {

	private String phaseName;
	private List<Milestone> milestones;
	
}
