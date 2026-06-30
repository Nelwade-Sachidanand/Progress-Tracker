package com.novillex.progresstracker.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "activity_history")
public class ActivityHistory {

    @Id
    private String id;

    private String projectId;

    private String requestId;

    private String phaseName;

    private String milestoneName;

    private String taskName;

    private String subTaskName;

    private String activityName;

    private Activity oldActivity;

    private Activity newActivity;

    private String approvedBy;

    private LocalDateTime approvedAt;

    private Boolean restored = false;

    private String restoredBy;

    private LocalDateTime restoredAt;
}