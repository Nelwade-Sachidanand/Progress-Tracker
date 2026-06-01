package com.dashboard.model;

import java.util.List;

import lombok.Data;

@Data
public class UserProjectUpdateModel {
	
	private String username;
	private List<String> projectNames;

}
