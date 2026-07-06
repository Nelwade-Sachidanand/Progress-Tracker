package com.novillex.progresstracker.entity;

import java.util.List;

import lombok.Data;

@Data
public class Task {
	private String taskId;
	private String taskName;
	private List<Subtask> subTasks;
}
