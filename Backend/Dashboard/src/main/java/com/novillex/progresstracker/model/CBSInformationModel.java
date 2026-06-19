package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class CBSInformationModel {

    private String previousCBSVendor;

    private String existingCBSVendor;

    private String cbsSince;

    private Integer totalUsers;

    private Integer totalAccounts;

    private String totalBusinessMix;

    private Boolean wantToChangeCBS;

    private String changeCBSWhen;
}