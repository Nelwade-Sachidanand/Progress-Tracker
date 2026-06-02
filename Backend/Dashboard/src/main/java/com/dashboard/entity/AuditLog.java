package com.dashboard.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    private String actionType;      

    private String entityType;      

    private String entityName;

    private String projectName;

    private String oldData;

    private String newData;

    private String modifiedBy;

    private LocalDateTime modifiedDate;
}