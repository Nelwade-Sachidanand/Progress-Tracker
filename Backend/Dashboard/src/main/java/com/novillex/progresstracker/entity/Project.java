package com.novillex.progresstracker.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "project_dashboard")
public class Project {

    @Id
    private String id;
    
    private String bankName;
    
    private String projectManager;

    private String projectName;
    
    private List<Phase> phases;
}