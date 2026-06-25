package com.novillex.progresstracker.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String title;

    private String message;

    private String type;

    private String referenceId;

    private String redirectUrl;

    private String createdBy;

    private String recipientUserId;

    private boolean read;

    private LocalDateTime createdAt;
}
