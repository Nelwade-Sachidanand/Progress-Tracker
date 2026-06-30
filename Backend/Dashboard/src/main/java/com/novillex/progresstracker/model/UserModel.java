package com.novillex.progresstracker.model;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
	
	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email address")
	private String email;
	
	@NotBlank(message = "Password is required")
    private String password;
	
	@NotBlank(message = "User role is required")
    private String role;
	
	@NotEmpty(message = "Assign at least one project to user")
    private List<String> projectIds;

    private Boolean status;

}
