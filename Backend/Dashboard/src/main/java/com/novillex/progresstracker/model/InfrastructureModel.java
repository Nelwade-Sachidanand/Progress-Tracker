package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class InfrastructureModel {

	private String currentLicenseType;

    private String currentDCVendor;

    private String currentDatabase;

    private String databaseVersion;
}