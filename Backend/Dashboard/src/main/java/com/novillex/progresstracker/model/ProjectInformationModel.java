package com.novillex.progresstracker.model;

import java.util.List;

import lombok.Data;

@Data
public class ProjectInformationModel {

    private String projectName;
    private String bankName;
    private String projectManager;
    private String salesPerson;

    private String headOfficeAddress;
    private String headOfficeContactNo;
    private Integer noOfBranches;
    private String bankType;

    private ContactModel contacts;

    private CBSInformationModel cbsInformation;

    private InfrastructureModel infrastructure;

    private ChannelsModel channels;

    private BusinessStatisticsModel businessStatistics;

    private List<HardwareDetailsModel> hardwareDetails;
}