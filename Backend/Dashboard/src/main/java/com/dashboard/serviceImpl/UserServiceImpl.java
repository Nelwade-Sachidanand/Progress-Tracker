package com.dashboard.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dashboard.common.AuditAction;
import com.dashboard.common.AuditEntity;
import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Project;
import com.dashboard.entity.User;
import com.dashboard.exception.DatabaseException;
import com.dashboard.exception.ResourceNotFoundException;
import com.dashboard.exception.ValidationException;
import com.dashboard.model.LoginModel;
import com.dashboard.model.LoginResponseModel;
import com.dashboard.model.UserModel;
import com.dashboard.model.UserUpdateModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.repository.UserRepository;
import com.dashboard.service.AuditService;
import com.dashboard.service.UserService;
import com.dashboard.util.JwtUtil;
import com.dashboard.util.UserContextUtil;

@Service
public class UserServiceImpl implements UserService {

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

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		User existingUser = userRepository.findByUsername(userModel.getUsername()).orElse(null);

		if(existingUser != null) {

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
		if(users.isEmpty()) {
			throw new ResourceNotFoundException("USR_404", "No users found", null);
		}
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Users fetched successfully", users);
	}

	@Override
	public Response login(LoginModel loginModel) {

		String username = loginModel.getUsername();
		String password = loginModel.getPassword();

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		User user = userRepository.findByUsername(username).orElse(null);
		System.out.println("find " + user);
		if(user == null) {
			throw new ResourceNotFoundException("USR_404", "User not found", username);
		}

		if(!Boolean.TRUE.equals(user.getActive())) {
			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE, "User is inactive",
					null);
		}

		boolean isPasswordValid = passwordEncoder.matches(password, user.getPassword());

		if(!isPasswordValid) {
			return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE,
					"Invalid username or password", null);
		}

		user.setPassword(null);
		List<Project> projects = new ArrayList<>();

		for(String projectName : user.getProjectNames()) {
			Project project = projectRepository.findByProjectName(projectName)
					.orElseThrow(() -> new ResourceNotFoundException("PRJ_404", " project not found", projectName));

			projects.add(project);
		}

		String token = JwtUtil.generateToken(username, user.getRole());

		LoginResponseModel responseModel = new LoginResponseModel();
		responseModel.setUser(user);
		responseModel.setProjects(projects);
		responseModel.setToken(token);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Login successful",
				responseModel);
	}

	/*
	 * @Override public Response updateUserProjects(UserProjectUpdateModel model) {
	 * System.out.println(model); ResponseBuilder responseBuilder =
	 * context.getBean(ResponseBuilder.class); User user =
	 * userRepository.findByUsername(model.getUsername()).orElse(null);
	 * 
	 * if(user == null) { throw new ResourceNotFoundException("USR_404",
	 * "User not found", model.getUsername()); }
	 * 
	 * User oldUser = new User();
	 * 
	 * BeanUtils.copyProperties(user, oldUser);
	 * 
	 * for(String projectName : model.getProjectNames()) { if
	 * (projectRepository.findByProjectName(projectName).isEmpty()) { throw new
	 * ResourceNotFoundException("PRJ_404", "Project not found", projectName); } }
	 * 
	 * user.setProjectNames(model.getProjectNames()); User updatedUser =
	 * userRepository.save(user);
	 * 
	 * if(!Objects.equals(oldUser.getProjectNames(),
	 * updatedUser.getProjectNames())) { String modifiedBy =
	 * SecurityContextHolder.getContext().getAuthentication().getName();
	 * auditService.saveAuditLog(AuditAction.UPDATE_USER, AuditEntity.USER,
	 * updatedUser.getUsername(), null, oldUser, updatedUser, modifiedBy); }
	 * updatedUser.setPassword(null); return
	 * responseBuilder.createResponse(StatusCode.SUCCESS,
	 * StatusCode.SUCCESS_STATUS_TYPE, "Projects updated successfully",
	 * updatedUser); }
	 * 
	 * @Override public Response updateUserStatus(String username, Boolean active) {
	 * 
	 * ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
	 * 
	 * if(username == null || username.isBlank()) { return
	 * responseBuilder.createResponse(StatusCode.ERROR,
	 * StatusCode.ERROR_STATUS_TYPE, "Username is required", null); }
	 * 
	 * User user = userRepository.findByUsername(username).orElse(null);
	 * 
	 * if(user == null) { throw new ResourceNotFoundException("USR_404",
	 * "User not found", username); }
	 * 
	 * User oldUser = new User();
	 * 
	 * BeanUtils.copyProperties(user, oldUser); if(user.getActive().equals(active))
	 * { return responseBuilder.createResponse(StatusCode.ERROR,
	 * StatusCode.ERROR_STATUS_TYPE, "User already has this status", null); }
	 * 
	 * user.setActive(active); userRepository.save(user);
	 * 
	 * if(!Objects.equals(oldUser.getActive(), user.getActive())) { String
	 * modifiedBy =
	 * SecurityContextHolder.getContext().getAuthentication().getName();
	 * auditService.saveAuditLog(AuditAction.CHANGE_STATUS, AuditEntity.USER,
	 * user.getUsername(), null, oldUser, user, modifiedBy); }
	 * 
	 * return responseBuilder.createResponse(StatusCode.SUCCESS,
	 * StatusCode.SUCCESS_STATUS_TYPE, "User status updated successfully", user); }
	 */
	@Override
	public Response updateUser(UserUpdateModel model) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		User user = userRepository.findByUsername(model.getUsername()).orElse(null);
		if(user == null) {

			throw new ResourceNotFoundException("USR_404", "User not found", model.getUsername());
		}

		User oldUser = new User();
		BeanUtils.copyProperties(user, oldUser);
		boolean isUpdated = false;

		// Update Projects
		if(model.getProjectNames() != null) {

			for(String projectName : model.getProjectNames()) {
				if(projectRepository.findByProjectName(projectName).isEmpty()) {
					throw new ResourceNotFoundException("PRJ_404", "Project not found", projectName);
				}
			}

			if(!Objects.equals(user.getProjectNames(), model.getProjectNames())) {
				user.setProjectNames(model.getProjectNames());
				isUpdated = true;
			}
		}

		if(model.getActive() != null && !Objects.equals(user.getActive(), model.getActive())) {
			user.setActive(model.getActive());
			isUpdated = true;
		}
		if(model.getRole() != null && !model.getRole().isBlank() && !Objects.equals(user.getRole(), model.getRole())) {
			user.setRole(model.getRole());
			isUpdated = true;
		}

		if(!isUpdated) {
			throw new ValidationException("VAL_013", "No changes found to update");
		}

		User updatedUser = userRepository.save(user);
		String modifiedBy = UserContextUtil.getCurrentUser();
		auditService.saveAuditLog(AuditAction.UPDATE_USER, AuditEntity.USER, updatedUser.getUsername(), null, oldUser,
				updatedUser, modifiedBy);

		updatedUser.setPassword(null);
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"User updated successfully", updatedUser);
	}

	@Override
	public Response deleteUser(String username) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		if(username == null || username.isBlank()) {
			throw new ValidationException("VAL_013", "Username is required");
		}

		User user = userRepository.findByUsername(username).orElse(null);
		if(user == null) {

			throw new ResourceNotFoundException("USR_404", "User not found", username);
		}
		User deletedUser = new User();
		BeanUtils.copyProperties(user, deletedUser);
		try{

			userRepository.delete(user);

		}catch(Exception e) {
			throw new DatabaseException("DB_002", "Unable to delete user");
		}

		String DeletedBy = UserContextUtil.getCurrentUser();
		auditService.saveAuditLog(AuditAction.DELETE_USER, AuditEntity.USER, deletedUser.getUsername(), null,
				deletedUser, null, DeletedBy);

		deletedUser.setPassword(null);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"User deleted successfully", deletedUser);
	}

}