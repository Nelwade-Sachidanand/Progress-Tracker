package com.novillex.progresstracker.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.LoginModel;
import com.novillex.progresstracker.model.LoginResponseModel;
import com.novillex.progresstracker.model.UserModel;
import com.novillex.progresstracker.model.UserUpdateModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.UserService;
import com.novillex.progresstracker.util.JwtUtil;
import com.novillex.progresstracker.util.UserContextUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private AuditService auditService;

	@Autowired
	private ProjectRepository projectRepository;

	@Override
	public Response register(UserModel userModel) {
		logger.info("User registration started. Username: {}", userModel.getUsername());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		User existingUser = userRepository.findByUsername(userModel.getUsername()).orElse(null);

		if (existingUser != null) {
			logger.warn("User registration failed. Username already exists: {}", userModel.getUsername());
			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE,
					"Username already exists", null);
		}
		if (userModel.getProjectIds() != null) {

			for (String projectId : userModel.getProjectIds()) {

				if (projectRepository.findById(projectId).isEmpty()) {

					throw new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", projectId);
				}
			}
		}
		User user = new User();

		BeanUtils.copyProperties(userModel, user);

		user.setPassword(passwordEncoder.encode(userModel.getPassword()));

		user.setStatus(true);

		User savedUser = userRepository.save(user);
		logger.info("User registered successfully. Username: {}", savedUser.getUsername());
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"User registered successfully", savedUser);
	}

	@Override
	public Response getAllUsers() {
		logger.info("Fetching all users");
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<User> users = userRepository.findAll();
		if (users.isEmpty()) {
			logger.warn("No users found");
			throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "No users found", null);
		}
		logger.info("Users fetched successfully. Count: {}", users.size());
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Users fetched successfully", users);
	}

	@Override
	public Response login(LoginModel loginModel) {

		String username = loginModel.getUsername();
		String password = loginModel.getPassword();
		logger.info("Login attempt for username: {}", username);
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		User user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			logger.warn("Login failed. User not found: {}", username);
			throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found", username);
		}

		if (!Boolean.TRUE.equals(user.getStatus())) {
			logger.warn("Login failed. User inactive: {}", username);
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

		
		  for (String projectId : user.getProjectIds()) {
		  
		  Project project = projectRepository.findById(projectId).orElseThrow( () ->
		  new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND,
		  "Project not found", projectId));
		  
		  projects.add(project); }
		 
		String accessToken = JwtUtil.generateAccessToken(user.getUsername(), user.getRole());

		String refreshToken = JwtUtil.generateRefreshToken(user.getUsername(),user.getRole());

		LoginResponseModel responseModel = new LoginResponseModel();
		responseModel.setUser(user);
		responseModel.setProjects(projects);
		responseModel.setAccessToken(accessToken);
		responseModel.setRefreshToken(refreshToken);
		
		logger.info("Login successful. Username: {}, Role: {}", username, user.getRole());
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Login successful",
				responseModel);
	}

	@Override
	public Response updateUser(UserUpdateModel model) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		User user = userRepository.findByUsername(model.getUsername()).orElse(null);
		if (user == null) {
			logger.warn("User update failed. User not found: {}", model.getUsername());
			throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found", model.getUsername());
		}

		User oldUser = new User();
		BeanUtils.copyProperties(user, oldUser);
		boolean isUpdated = false;

		if (model.getProjectIds() != null) {

			for (String projectId : model.getProjectIds()) {

				if (projectRepository.findById(projectId).isEmpty()) {

					logger.warn("User update failed. Project not found: {}", projectId);

					throw new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", projectId);
				}
			}

			if (!Objects.equals(user.getProjectIds(), model.getProjectIds())) {

				user.setProjectIds(model.getProjectIds());

				isUpdated = true;
			}
		}
		if (model.getFullname() != null && !model.getFullname().isBlank()
				&& !Objects.equals(user.getFullname(), model.getFullname())) {

			user.setFullname(model.getFullname());

			isUpdated = true;
		}
		if (model.getActive() != null && !Objects.equals(user.getStatus(), model.getActive())) {
			user.setStatus(null);
			isUpdated = true;
		}
		if (model.getRole() != null && !model.getRole().isBlank() && !Objects.equals(user.getRole(), model.getRole())) {
			user.setRole(model.getRole());
			isUpdated = true;
		}

		if (!isUpdated) {
			logger.warn("No changes found for user: {}", model.getUsername());
			throw new ValidationException(ErrorCode.NO_CHANGES_FOUND, "No changes found to update");
		}

		User updatedUser = userRepository.save(user);
		String modifiedBy = UserContextUtil.getCurrentUser();
		auditService.saveAuditLog(AuditAction.UPDATE_USER, AuditEntity.USER, updatedUser.getUsername(), null, oldUser,
				updatedUser, modifiedBy);
		logger.info("User updated successfully. Username: {}, Modified By: {}", updatedUser.getUsername(), modifiedBy);
		updatedUser.setPassword(null);
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"User updated successfully", updatedUser);
	}

	@Override
	public Response deleteUser(String username) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		if (username == null || username.isBlank()) {
			logger.warn("Delete user failed. Username is empty");
			throw new ValidationException(ErrorCode.USERNAME_REQUIRED, "Username is required");
		}

		User user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			logger.warn("Delete user failed. User not found: {}", username);
			throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found", username);
		}
		User deletedUser = new User();
		BeanUtils.copyProperties(user, deletedUser);
		try {

			userRepository.delete(user);

		} catch (Exception e) {
			logger.error("Failed to delete user: {}", username, e);
			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to delete user");
		}

		String DeletedBy = UserContextUtil.getCurrentUser();
		auditService.saveAuditLog(AuditAction.DELETE_USER, AuditEntity.USER, deletedUser.getUsername(), null,
				deletedUser, null, DeletedBy);

		deletedUser.setPassword(null);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"User deleted successfully", deletedUser);
	}

}