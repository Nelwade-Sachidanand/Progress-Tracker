package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class HardwareDetailsModel {

	private String serverType;

    private Integer units;

    private Integer diskSpaceGb;

    private Integer ramGb;

    private Integer cores;
}