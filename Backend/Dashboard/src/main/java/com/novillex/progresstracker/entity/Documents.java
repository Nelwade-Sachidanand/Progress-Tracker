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

    private String projectId;

    private String projectName;

    private String bankName;

    private String phaseId;

    private String milestoneId;

    private String taskId;

    private String subTaskId;

    private String activityId;

    private List<ActivityDocument> documents = new ArrayList<>();
}