package com.novillex.progresstracker.model;

import java.util.List;

import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseModel {
	private User user;
	private List<Project> projects;
	private String accessToken;
    private String refreshToken;
}
