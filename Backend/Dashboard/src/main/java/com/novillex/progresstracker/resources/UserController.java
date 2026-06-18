package com.novillex.progresstracker.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.LoginModel;
import com.novillex.progresstracker.model.UserModel;
import com.novillex.progresstracker.model.UserUpdateModel;
import com.novillex.progresstracker.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserService userService;

	@PostMapping("/register")
	public Response registerUser(@RequestBody UserModel userModel) {

		logger.info("User registration request received. Username: {}", userModel.getUsername());

		return userService.register(userModel);
	}

	@PostMapping("/login")
	public Response login(@RequestBody LoginModel loginModel) {

		logger.info("Login request received. Username: {}", loginModel.getUsername());

		return userService.login(loginModel);
	}

	@GetMapping("/getAllUsers")
	public Response getAllUsers() {

		logger.info("Get all users request received");

		return userService.getAllUsers();
	}

	@PutMapping("/updateUser")
	public Response updateUser(@RequestBody UserUpdateModel model) {

		logger.info("Update user request received. Username: {}", model.getUsername());

		return userService.updateUser(model);
	}

	@DeleteMapping("/deleteUser/{username}")
	public Response deleteUser(@PathVariable String username) {

		logger.info("Delete user request received. Username: {}", username);

		return userService.deleteUser(username);
	}
}