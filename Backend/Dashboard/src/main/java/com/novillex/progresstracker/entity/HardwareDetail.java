package com.novillex.progresstracker.entity;

import lombok.Data;

@Data
public class HardwareDetail {

    private String serverType;

    private Integer units;

    private Integer diskSpaceGb;

    private Integer ramGb;

    private Integer cores;
}