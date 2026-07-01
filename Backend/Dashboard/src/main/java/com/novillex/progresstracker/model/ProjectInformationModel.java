package com.novillex.progresstracker.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class ProjectInformationModel {

    @Id
    private String id;

    // Bank Details
    @NotBlank(message = "Project Name is required")
    private String projectName;
    
    @NotBlank(message = "Bank Name is required")
    private String bankName;
    private String projectManager;
    private String salesPerson;
    private String headOfficeAddress;
    private String headOfficeContactNo;
    private Integer noOfBranches;
    
    @NotBlank(message = "Type Of Bank is required")
    private String bankType;

    // Management Details
    private ContactModel contactDetails;

    // CBS Information
    private CBSInformationModel cbsInformation;

    // Business Statistics
    private BusinessStatisticsModel businessStatistics;

    // Infrastructure
    private InfrastructureModel infrastructure;

    // Server Configuration
    private List<HardwareDetailsModel> hardwareDetails;

    // Digital Channels
    private DigitalChannelsModel digitalChannels;

    // Payment Systems
    private PaymentSystemsModel paymentSystems;

    private String status;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}