package com.dashboard.service;

import com.dashboard.common.Response;
import com.dashboard.model.LoginModel;
import com.dashboard.model.UserModel;

public interface UserService {
    Response register(UserModel userModel);

    Response getAllUsers();

    Response login(LoginModel loginModel);

}
