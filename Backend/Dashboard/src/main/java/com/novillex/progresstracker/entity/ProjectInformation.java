package com.novillex.progresstracker.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "project_information")
@Data
public class ProjectInformation {

    @Id
    private String id;

    private String projectName;
    private String bankName;
    private String projectManager;
    private String salesPerson;

    private String headOfficeAddress;
    private String headOfficeContactNo;
    private Integer noOfBranches;
    private String bankType;

    private ContactDetails contacts;

    private CbsInformation cbsInformation;

    private Infrastructure infrastructure;

    private Channels channels;

    private BusinessStatistics businessStatistics;

    private List<HardwareDetail> hardwareDetails;

    private String status;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}