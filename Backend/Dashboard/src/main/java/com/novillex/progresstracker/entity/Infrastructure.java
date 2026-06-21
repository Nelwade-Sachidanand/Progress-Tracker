package com.novillex.progresstracker.entity;

import lombok.Data;

@Data
public class Infrastructure {

    private String currentLicenseType;

    private String currentDCVendor;

    private String currentDatabase;

    private String databaseVersion;
}