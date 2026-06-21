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

    // Bank Details
    private String projectName;
    private String bankName;
    private String projectManager;
    private String salesPerson;
    private String headOfficeAddress;
    private String headOfficeContactNo;
    private Integer noOfBranches;
    private String bankType;

    // Management Details
    private ContactDetails contactDetails;

    // CBS Information
    private CbsInformation cbsInformation;

    // Business Statistics
    private BusinessStatistics businessStatistics;

    // Infrastructure
    private Infrastructure infrastructure;

    // Server Configuration
    private List<HardwareDetail> hardwareDetails;

    // Digital Channels
    private DigitalChannels digitalChannels;

    // Payment Systems
    private PaymentSystems paymentSystems;

    private String status;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}