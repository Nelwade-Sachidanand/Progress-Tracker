package com.novillex.progresstracker.entity;

import lombok.Data;

@Data
public class BusinessStatistics {

    private Integer totalActiveCustomers;

    private Integer accountsAcrossBusinesses;

    private Integer dailyTransactions;

    private Integer digitalTransactions;

    private Integer upiTransactions;

    private Integer totalUsers;

    private Integer concurrentUsers;

    private Integer accountsIncreasedPerYear;

    private String otherInformation;
}