package com.novillex.progresstracker.model;

import lombok.Data;

@Data
public class RollbackRequestModel {

    private String password;

    private String reason;
}