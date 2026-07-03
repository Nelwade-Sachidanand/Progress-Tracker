package com.novillex.progresstracker.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {
	@NotBlank(message = "User Id is required.")
	private String userId;

	@NotBlank(message = "Password is required.")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&^#()_+=-])[A-Za-z\\d@$!%*?&^#()_+=-]{8,20}$", message = "Password must contain minimum 8 characters, one uppercase, one lowercase, one number and one special character.")
	private String newPassword;
	
	@NotBlank(message = "Confirm Password is required.")
    private String confirmPassword;
}
