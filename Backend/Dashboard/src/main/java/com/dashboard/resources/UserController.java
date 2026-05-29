package com.dashboard.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.common.Response;
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
	
	@GetMapping("/login/{userName}/{password}")
	public Response login(@PathVariable String userName, @PathVariable String password) {
		System.out.println(userName);
		return userService.login(userName,password);
	}
	
	@GetMapping("/getAllUsers")
	public Response getAllUsers() {
	return userService.getAllUsers();
	}
}
