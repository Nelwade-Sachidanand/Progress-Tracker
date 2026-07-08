package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.LoginModel;
import com.novillex.progresstracker.model.LoginResponseModel;
import com.novillex.progresstracker.model.ResetPasswordRequest;
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

	private UserRepository userRepository;

	private PasswordEncoder passwordEncoder;

	private ApplicationContext context;

	private AuditService auditService;

	private ProjectRepository projectRepository;

	public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, ApplicationContext context,
			AuditService auditService, ProjectRepository projectRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.context = context;
		this.auditService = auditService;
		this.projectRepository = projectRepository;
	}

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

		if (Boolean.TRUE.equals(user.getLoggedIn())) {

			logger.warn("Login denied. User '{}' is already logged in.", username);

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE,
					"User is already logged in", null);
		}

		boolean isPasswordValid = passwordEncoder.matches(password, user.getPassword());

		if (!isPasswordValid) {

			logger.warn("Login failed. Invalid password for username: {}", username);

			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE, "Invalid password",
					null);
		}

		user.setLoggedIn(true);
		user.setSessionId(UUID.randomUUID().toString());
		user.setLoginTime(LocalDateTime.now());

		userRepository.save(user);

		String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());

		String refreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

		user.setPassword(null);

		LoginResponseModel responseModel = new LoginResponseModel();
		responseModel.setUser(user);
		responseModel.setAccessToken(accessToken);
		responseModel.setRefreshToken(refreshToken);

		logger.info("Login successful. Username: {}, Role: {}", username, user.getRole());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Login successful",
				responseModel);
	}

	@Override
	public Response logout() {

		logger.info("Logout initiated for user: {}", UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		User user = userRepository.findByUsername(UserContextUtil.getCurrentUser())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found",
						UserContextUtil.getCurrentUser()));

		user.setLoggedIn(false);
		user.setSessionId(null);
		user.setLoginTime(null);

		userRepository.save(user);

		logger.info("Logout successful for user: {}", user.getUsername());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Logout successful.",
				null);
	}

	@Override
	public Response updateUser(UserUpdateModel model) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		User user = userRepository.findById(model.getUserId()).orElse(null);
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
		if (model.getUsername() != null && !model.getUsername().isBlank()
				&& !Objects.equals(user.getUsername(), model.getUsername())) {
			user.setUsername(model.getUsername());

			isUpdated = true;
		}
		if (model.getEmail() != null && !model.getEmail().isBlank()
				&& !Objects.equals(user.getEmail(), model.getEmail())) {
			user.setEmail(model.getEmail());

			isUpdated = true;
		}
		if (model.getPassword() != null && !model.getPassword().isBlank()
				&& !Objects.equals(user.getPassword(), model.getPassword())) {
			user.setPassword(passwordEncoder.encode(model.getPassword()));

			isUpdated = true;
		}
		if (model.getFullname() != null && !model.getFullname().isBlank()
				&& !Objects.equals(user.getFullname(), model.getFullname())) {

			user.setFullname(model.getFullname());

			isUpdated = true;
		}
		if (model.getStatus() != null && !Objects.equals(user.getStatus(), model.getStatus())) {
			user.setStatus(model.getStatus());
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
	public Response deleteUser(String userId) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		if (userId == null || userId.isBlank()) {
			logger.warn("Delete user failed. userId is empty");
			throw new ValidationException(ErrorCode.USERNAME_REQUIRED, "Username is required");
		}

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			logger.warn("Delete user failed. User not found: {}", userId);
			throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found", userId);
		}
		User deletedUser = new User();
		BeanUtils.copyProperties(user, deletedUser);
		try {

			userRepository.delete(user);

		} catch (Exception e) {
			logger.error("Failed to delete user: {}", userId, e);
			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to delete user");
		}

		String DeletedBy = UserContextUtil.getCurrentUser();
		auditService.saveAuditLog(AuditAction.DELETE_USER, AuditEntity.USER, deletedUser.getUsername(), null,
				deletedUser, null, DeletedBy);

		deletedUser.setPassword(null);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"User deleted successfully", deletedUser);
	}

	@Override
	public Response resetPassword(ResetPasswordRequest request) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		if (!request.getNewPassword().equals(request.getConfirmPassword())) {

			throw new ValidationException(ErrorCode.PASSWORD_MISMATCH,
					"New Password and Confirm Password do not match.");
		}

		if (request.getNewPassword().contains(" ")) {
			throw new ValidationException(ErrorCode.INVALID_PASSWORD, "Password should not contain spaces.");
		}

		User user = userRepository.findById(request.getUserId()).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found.", request.getUserId()));

		if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {

			throw new ValidationException(ErrorCode.PASSWORD_ALREADY_USED,
					"New password cannot be same as current password.");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));

		userRepository.save(user);

		auditService.saveAuditLog(AuditAction.RESET_PASSWORD, AuditEntity.USER, user.getUsername(), null, null, null,
				UserContextUtil.getCurrentUser());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Password reset successfully.", null);
	}

}