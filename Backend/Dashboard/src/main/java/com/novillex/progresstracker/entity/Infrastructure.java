package com.novillex.progresstracker.entity;

import lombok.Data;

@Data
public class Infrastructure {

    private String licenseType;

    private String currentDcDrVendor;

    private String currentDatabase;

    private Integer customerOnboardingPerMonth;

    private Integer loanIssuePerMonth;
}