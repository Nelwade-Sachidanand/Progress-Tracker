package com.novillex.progresstracker.model;

import java.util.List;

import lombok.Data;

@Data
public class BulkAuthRequestModel {
	private List<String> requestIds;

    private String reason;

}
