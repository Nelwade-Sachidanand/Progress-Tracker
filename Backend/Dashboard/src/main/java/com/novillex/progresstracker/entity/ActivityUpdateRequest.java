package com.novillex.progresstracker.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "activity_update_requests")
public class ActivityUpdateRequest {

    @Id
    private String id;

    private String projectId;

    private String phaseName;

    private String milestoneName;

    private String taskName;

    private String subTaskName;

    private String activityName;

    private Activity oldActivity;

    private Activity newActivity;

    private String requestedBy;

    private String approvedBy;

    private String status;

    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private String rejectionReason;
    
    private String requestSource;
}