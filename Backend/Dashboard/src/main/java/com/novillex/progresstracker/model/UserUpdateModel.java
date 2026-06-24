package com.novillex.progresstracker.model;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateModel {
	
	@NotBlank(message = "Full Name is required")
	private String fullname;
	
	@NotBlank(message = "Username is required")
    private String username;
	
	@NotBlank(message = "Assign at least one project to user")
    private List<String> projectIds;

	
    private Boolean active;
    
    @NotBlank(message = "User role is required")
    private String role;
}