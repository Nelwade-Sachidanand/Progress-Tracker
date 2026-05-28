package com.dashboard.entity;

import java.util.List;

import lombok.Data;

@Data
public class Milestone {

	private String milestoneName;
	private List<Task> tasks;

}
