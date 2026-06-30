package com.novillex.progresstracker.entity;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class BusinessStatistics {
	
	@Min(value = 0, message = "Total Active Users cannot be negative")
    private Integer totalActiveCustomers;
	
	@Min(value = 0, message = "Total Accounts cannot be negative")
    private Integer totalAccounts;
    
    @Min(value = 0, message = "Total Users cannot be negative")
    private Integer totalUsers;
    
    @Min(value = 0, message = "Concurrent Users cannot be negative")
    private Integer concurrentUsers;
    
    @Min(value = 0, message = "Accounts Increased/year cannot be negative")
    private Integer accountsPerYear;
    
    @Min(value = 0, message = "Daily Transactions cannot be negative")
    private Integer dailyTransactions;
    
    @Min(value = 0, message = "Digital Transactions cannot be negative")
    private Integer digitalTransactions;
    
    @Min(value = 0, message = "Upi Transactions cannot be negative")
    private Integer upiTransactions;
    
    @Min(value = 0, message = "Business Mix cannot be negative")
    private String businessMix;
    
    @Min(value = 0, message = "Customer Onboarding cannot be negative")
    private Integer customerOnboarding;
    
    @Min(value = 0, message = "Loan issues cannot be negative")
    private Integer loanIssues;
}