package com.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogModel {

    private String actionType;

    private String entityType;

    private String entityName;

    private String projectName;

    private Object oldData;

    private Object newData;
}