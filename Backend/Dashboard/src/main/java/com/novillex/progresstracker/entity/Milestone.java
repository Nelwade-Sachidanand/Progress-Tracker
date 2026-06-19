package com.novillex.progresstracker.entity;

import java.util.List;

import lombok.Data;

@Data
public class Milestone {

	private String milestoneName;
	
    private Double weightage;

	private List<Task> tasks;

}
