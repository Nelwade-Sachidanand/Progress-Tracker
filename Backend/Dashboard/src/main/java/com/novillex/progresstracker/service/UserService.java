package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.LoginModel;
import com.novillex.progresstracker.model.UserModel;
import com.novillex.progresstracker.model.UserUpdateModel;

public interface UserService {
	Response register(UserModel userModel);

	Response getAllUsers();

	Response login(LoginModel loginModel);

	/*
	 * Response updateUserProjects(UserProjectUpdateModel model);
	 * 
	 * Response updateUserStatus(String username, Boolean active);
	 */
	
	Response updateUser(UserUpdateModel model);
	
	Response deleteUser(String username);

}
