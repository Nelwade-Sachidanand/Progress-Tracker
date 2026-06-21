package com.novillex.progresstracker.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;


@Data
public class ProjectInformationModel {

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