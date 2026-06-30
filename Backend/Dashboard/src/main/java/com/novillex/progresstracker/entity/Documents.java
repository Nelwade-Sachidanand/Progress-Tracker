package com.novillex.progresstracker.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "Documents")
public class Documents {
	@Id
	private String id;

	private String projectName;
	private String phaseName;
	private String milestoneName;
	private String taskName;
	private String subTaskName;
	private String activityName;

	private List<ActivityDocument> documents = new ArrayList<>();
}
