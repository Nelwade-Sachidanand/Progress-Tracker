package com.novillex.progresstracker.entity;

import lombok.Data;

@Data
public class CbsInformation {

    private String previousCBSVendor;

    private String existingCBSVendor;

    private String cbsSince;

    private Integer totalUsers;

    private Integer totalAccounts;

    private String totalBusinessMix;

    private Boolean wantToChangeCBS;

    private String changeCBSWhen;
}