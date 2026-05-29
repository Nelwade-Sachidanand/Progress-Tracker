package com.dashboard.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Project;
import com.dashboard.entity.User;
import com.dashboard.model.LoginResponseModel;
import com.dashboard.model.UserModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.repository.UserRepository;
import com.dashboard.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ProjectRepository projectRepository;

	@Override
	public Response register(UserModel userModel) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		User existingUser = userRepository.findByUsername(userModel.getUsername()).orElse(null);

		if (existingUser != null) {

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE,
					"Username already exists", null);
		}

		User user = new User();

		BeanUtils.copyProperties(userModel, user);

		user.setPassword(passwordEncoder.encode(userModel.getPassword()));

		user.setActive(true);

		User savedUser = userRepository.save(user);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"User registered successfully", savedUser);
	}

	@Override
	public Response getAllUsers() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<User> users = userRepository.findAll();

		if (users.isEmpty()) {

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE, "No users found",
					null);
		}

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Users fetched successfully", users);
	}

	@Override
	public Response login(String username, String password) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		User user = userRepository.findByUsername(username).orElse(null);
		
		System.out.println("find");

		if (user == null) {

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE, "User not found",
					null);
		}

		if (!Boolean.TRUE.equals(user.getActive())) {

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE, "User is inactive",
					null);
		}

		boolean isPasswordValid = passwordEncoder.matches(password, user.getPassword());

		if (!isPasswordValid) {

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE,
					"Invalid username or password", null);
		}

		user.setPassword(null);
		List<Project> projects = new ArrayList<>();
		for (String projectName : user.getProjectNames()) {
			Project project = projectRepository.findByProjectName(projectName).get();
			projects.add(project);
		}
		LoginResponseModel responseModel = new LoginResponseModel();
		responseModel.setUser(user);
		responseModel.setProjects(projects);
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Login successful",
				responseModel);
	}
}