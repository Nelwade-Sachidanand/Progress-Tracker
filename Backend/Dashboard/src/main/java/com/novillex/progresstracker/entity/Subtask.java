package com.novillex.progresstracker.entity;

import java.util.List;

import lombok.Data;

@Data
public class Subtask {
	
	private String subTaskName;
	private List<Activity> activities;
}
