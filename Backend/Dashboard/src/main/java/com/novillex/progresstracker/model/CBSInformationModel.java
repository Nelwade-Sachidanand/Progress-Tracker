package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class CBSInformationModel {

	private String previousCBSVendor;

    private String previousVendorPeriod;

    private String existingCBSVendor;

    private String cbsSince;
}