package com.dashboard.service;

import com.dashboard.common.Response;
import com.dashboard.model.LoginModel;
import com.dashboard.model.UserModel;
import com.dashboard.model.UserProjectUpdateModel;

public interface UserService {
	Response register(UserModel userModel);

	Response getAllUsers();

	Response login(LoginModel loginModel);

	Response updateUserProjects(UserProjectUpdateModel model);

	Response updateUserStatus(String username, Boolean active);

}
