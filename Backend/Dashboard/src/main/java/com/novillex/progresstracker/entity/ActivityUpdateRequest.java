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

    // =========================
    // Hierarchy IDs
    // =========================

    private String phaseId;
    private String milestoneId;
    private String taskId;
    private String subTaskId;
    private String activityId;

    // =========================
    // Old Hierarchy
    // =========================

    private String oldPhaseName;
    private String oldMilestoneName;
    private String oldTaskName;
    private String oldSubTaskName;
    private String oldOwner;
    private String oldActivityName;

    // =========================
    // New Hierarchy
    // =========================

    private String newPhaseName;
    private String newMilestoneName;
    private String newTaskName;
    private String newSubTaskName;
    private String newOwner;
    private String newActivityName;

    // =========================
    // Activity Snapshot
    // =========================

    private Activity oldActivity;
    private Activity newActivity;

    // =========================
    // Request Details
    // =========================

    /**
     * UI
     * EXCEL
     * API
     */
    private String requestSource;

    /**
     * CREATE
     * UPDATE
     * DELETE
     */
    private String requestType;

    /**
     * PENDING
     * APPROVED
     * REJECTED
     * ROLLED_BACK
     */
    private String status;

    private String changeReason;

    // =========================
    // Requested By
    // =========================

    private String requestedBy;

    private String requestedByUserId;

    private LocalDateTime requestedAt;

    // =========================
    // Approval
    // =========================

    private String approvedBy;

    private String approvedByUserId;

    private LocalDateTime approvedAt;

    // =========================
    // Rejection
    // =========================

    private String rejectionReason;

    // =========================
    // Rollback
    // =========================

    private String rollbackReason;

    private String rolledBackBy;

    private LocalDateTime rolledBackAt;
    
    private String requestedByRole;
    
}