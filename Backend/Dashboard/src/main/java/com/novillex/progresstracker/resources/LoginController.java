package com.novillex.progresstracker.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.LoginModel;
import com.novillex.progresstracker.service.UserService;
import com.novillex.progresstracker.util.UserContextUtil;

@RestController
@RequestMapping("/user")
public class LoginController {
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	private UserService userService;
	
	public LoginController(UserService userService) {
		this.userService=userService;
	}
	
	@PostMapping("/login")
	public Response login(@RequestBody LoginModel loginModel) {

		logger.info("Login request received. Username: {}", loginModel.getUsername());

		return userService.login(loginModel);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/logout")
	public Response logout() {

	    logger.info("Logout request received for user: {}", UserContextUtil.getCurrentUser());

	    return userService.logout();
	}
}
