package com.novillex.progresstracker.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserModel {
	
	private String fullname;
	
    private String username;

    private String password;

    private String role;

    private List<String> projectNames;

    private Boolean active;

}
