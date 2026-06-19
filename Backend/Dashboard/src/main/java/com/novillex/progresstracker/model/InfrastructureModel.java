package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class InfrastructureModel {

    private String licenseType;

    private String currentDcDrVendor;

    private String currentDatabase;

    private Integer customerOnboardingPerMonth;

    private Integer loanIssuePerMonth;
}