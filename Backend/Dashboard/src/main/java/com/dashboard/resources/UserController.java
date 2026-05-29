package com.dashboard.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.common.Response;
import com.dashboard.model.LoginModel;
import com.dashboard.model.UserModel;
import com.dashboard.service.UserService;

@RestController
@RequestMapping("/dashboard")
public class UserController {
	
	@Autowired
	private UserService userService;
	
	@PostMapping("/register/user")
	public Response registerUser(@RequestBody UserModel userModel) {
		return userService.register(userModel);
	}
	
	@PostMapping("/login")
	public Response login(@RequestBody LoginModel loginModel) {
		System.out.println(loginModel);
		return userService.login(loginModel);
	}
	
	@GetMapping("/getAllUsers")
	public Response getAllUsers() {
	return userService.getAllUsers();
	}
}
