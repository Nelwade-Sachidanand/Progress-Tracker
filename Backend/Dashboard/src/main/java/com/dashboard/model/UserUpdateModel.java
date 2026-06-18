package com.dashboard.model;

import java.util.List;

import lombok.Data;

@Data
public class UserUpdateModel {
	
	private String fullname;

    private String username;

    private List<String> projectIds;

    private Boolean active;

    private String role;
}