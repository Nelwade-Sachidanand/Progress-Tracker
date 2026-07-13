package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ChangeTemporaryPasswordRequest;
import com.novillex.progresstracker.model.LoginModel;
import com.novillex.progresstracker.model.ResetPasswordRequest;
import com.novillex.progresstracker.model.UserModel;
import com.novillex.progresstracker.model.UserUpdateModel;

public interface UserService {
	Response register(UserModel userModel);

	Response getAllUsers();

	Response login(LoginModel loginModel);
	
	//Response logout();

	/*
	 * Response updateUserProjects(UserProjectUpdateModel model);
	 * 
	 * Response updateUserStatus(String username, Boolean active);
	 */
	
	Response updateUser(UserUpdateModel model);
	
	Response deleteUser(String userId);
	
	Response resetPassword(ResetPasswordRequest request);
	
	Response forgotPassword(String username);

	Response getForgotPasswordRequests();

	Response generateTemporaryPassword(String userId, String temporaryPassword);

	Response changeTemporaryPassword(ChangeTemporaryPasswordRequest request);

}
