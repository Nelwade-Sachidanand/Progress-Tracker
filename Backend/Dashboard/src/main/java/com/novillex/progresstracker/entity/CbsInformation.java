package com.novillex.progresstracker.entity;

import lombok.Data;

@Data
public class CbsInformation {

    private String previousCBSVendor;

    private String previousVendorPeriod;

    private String existingCBSVendor;

    private String cbsSince;
}