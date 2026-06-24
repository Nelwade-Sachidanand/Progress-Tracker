package com.novillex.progresstracker.model;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserModel {
	
	@NotBlank(message = "Full Name is required")
	private String fullname;
	
	@NotBlank(message = "Usename is required")
    private String username;
	
	@NotBlank(message = "Password is required")
    private String password;
	
	@NotBlank(message = "User role is required")
    private String role;
	
	@NotBlank(message = "Assign at least one project to user")
    private List<String> projectIds;

    private Boolean status;

}
