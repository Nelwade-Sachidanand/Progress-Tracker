package com.novillex.progresstracker.model;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UserUpdateModel {
	
	private String userId;
	
	@NotBlank(message = "Full Name is required")
	private String fullname;
	
	@NotBlank(message = "Username is required")
    private String username;
	
	@NotBlank(message = "Password is required")
    private String password;

	@NotEmpty(message = "Assign at least one project to user")
	private List<String> projectIds;

    private Boolean active;
    
    @NotBlank(message = "User role is required")
    private String role;
}