package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class BusinessStatisticsModel {

	private Integer totalActiveCustomers;

    private Integer totalAccounts;

    private Integer totalUsers;

    private Integer concurrentUsers;

    private Integer accountsPerYear;

    private Integer dailyTransactions;

    private Integer digitalTransactions;

    private Integer upiTransactions;

    private String businessMix;

    private Integer customerOnboarding;

    private Integer loanIssues;
}