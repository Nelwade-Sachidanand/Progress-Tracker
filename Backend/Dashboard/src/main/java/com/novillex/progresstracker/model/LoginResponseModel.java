package com.novillex.progresstracker.model;


import com.novillex.progresstracker.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseModel {
	private User user;
	private String accessToken;
    private String refreshToken;
    private Boolean forcePasswordChange;
}
