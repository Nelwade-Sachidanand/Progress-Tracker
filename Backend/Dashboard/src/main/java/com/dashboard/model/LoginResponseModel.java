package com.dashboard.model;

import java.util.List;

import com.dashboard.entity.Project;
import com.dashboard.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseModel {
	private User user;
	private List<Project> projects;
	private String token;
}
