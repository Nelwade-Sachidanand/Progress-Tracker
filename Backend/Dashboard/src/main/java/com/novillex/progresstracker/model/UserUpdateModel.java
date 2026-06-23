package com.novillex.progresstracker.model;

import java.util.List;

import lombok.Data;

@Data
public class UserUpdateModel {
	
	private String userId;
	private String fullname;

    private String username;
    private String password;

    private List<String> projectIds;

    private Boolean active;

    private String role;
}