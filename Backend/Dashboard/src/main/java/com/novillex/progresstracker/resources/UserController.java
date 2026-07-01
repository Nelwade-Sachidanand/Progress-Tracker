package com.novillex.progresstracker.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.model.UserModel;
import com.novillex.progresstracker.model.UserUpdateModel;
import com.novillex.progresstracker.service.UserService;
import com.novillex.progresstracker.util.JwtUtil;

import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/user")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private UserService userService;
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/register")
	public Response registerUser(@RequestBody UserModel userModel) {

		logger.info("User registration request received. Username: {}", userModel.getUsername());

		return userService.register(userModel);
	}
	
	
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/getAllUsers")
	public Response getAllUsers() {

		logger.info("Get all users request received");

		return userService.getAllUsers();
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/updateUser")
	public Response updateUser(@RequestBody UserUpdateModel model) {

		logger.info("Update user request received. Username: {}", model.getUsername());

		return userService.updateUser(model);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/deleteUser/{userId}")
	public Response deleteUser(@PathVariable String userId) {

		logger.info("Delete user request received. Username: {}", userId);

		return userService.deleteUser(userId);
	}

	@PostMapping("/refresh")
	public Response refreshToken(@RequestParam String refreshToken) {
		
		logger.info("Refreshing Token");

		Claims claims = JwtUtil.extractClaims(refreshToken);

		String username = claims.getSubject();

		String role = (String) claims.get("role");
		
		String userId = (String) claims.get("userId");

		String newAccessToken = JwtUtil.generateAccessToken(userId,username, role);

		return new Response(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Token refreshed successfully",
				newAccessToken);
	}
}