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
    private String projectName;


    private String phaseId;
    private String milestoneId;
    private String taskId;
    private String subTaskId;
    private String activityId;


    private String oldPhaseName;
    private String oldMilestoneName;
    private String oldTaskName;
    private String oldSubTaskName;
    private String oldOwner;
    private String oldActivityName;


    private String newPhaseName;
    private String newMilestoneName;
    private String newTaskName;
    private String newSubTaskName;
    private String newOwner;
    private String newActivityName;

    private Activity oldActivity;
    private Activity newActivity;

    private String requestSource;

    private String requestType;

    private String status;

    private String changeReason;

    private String requestedBy;

    private String requestedByUserId;

    private LocalDateTime requestedAt;

    private String approvedBy;

    private String approvedByUserId;

    private LocalDateTime approvedAt;
    
    private String rejectionReason;

    private String rollbackReason;

    private String rolledBackBy;

    private LocalDateTime rolledBackAt;
    
    private String requestedByRole;
    
}