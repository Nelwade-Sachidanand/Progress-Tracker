package com.dashboard.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.common.Response;
import com.dashboard.model.LoginModel;
import com.dashboard.model.UserModel;
import com.dashboard.model.UserProjectUpdateModel;
import com.dashboard.model.UserUpdateModel;
import com.dashboard.service.UserService;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

	@Autowired
	private UserService userService;

	@PostMapping("/register")
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

	/*
	 * @PutMapping("/updateProjects") public Response
	 * updateUserProjects(@RequestBody UserProjectUpdateModel model) { return
	 * userService.updateUserProjects(model); }
	 * 
	 * @PutMapping("/updateStatus") public Response updateUserStatus(@RequestParam
	 * String username, @RequestParam Boolean active) { return
	 * userService.updateUserStatus(username, active); }
	 */
	@PutMapping("/updateUser")
	public Response updateUser(@RequestBody UserUpdateModel model) {
		return userService.updateUser(model);
	}

	@DeleteMapping("/deleteUser/{username}")
	public Response deleteUser(@PathVariable String username) {
		return userService.deleteUser(username);
	}
}
