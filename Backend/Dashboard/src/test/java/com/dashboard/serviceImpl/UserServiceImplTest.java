package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.LoginModel;
import com.novillex.progresstracker.model.UserModel;
import com.novillex.progresstracker.model.UserUpdateModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.serviceImpl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationContext context;

	@Mock
	private AuditService auditService;

	@Mock
	private ProjectRepository projectRepository;

	@InjectMocks
	private UserServiceImpl userService;

	@Test
	void register_UsernameAlreadyExists() {

		UserModel model = new UserModel();
		model.setUsername("admin");

		User existingUser = new User();

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));

		userService.register(model);

		verify(userRepository, never()).save(any());
	}

	@Test
	void register_Success() {

		UserModel model = new UserModel();
		model.setUsername("admin");
		model.setPassword("123");
		model.setProjectIds(List.of("P1"));

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		Project project = new Project();
		User savedUser = new User();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

		when(projectRepository.findById("P1")).thenReturn(Optional.of(project));

		when(passwordEncoder.encode("123")).thenReturn("encodedPassword");

		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = userService.register(model);

		assertNotNull(result);

		verify(userRepository).save(any(User.class));
	}

	@Test
	void register_ProjectNotFound() {

		UserModel model = new UserModel();
		model.setUsername("admin");
		model.setProjectIds(List.of("P1"));

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

		when(projectRepository.findById("P1")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.register(model));
	}

	@Test
	void getAllUsers_Success() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		List<User> users = List.of(new User(), new User());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findAll()).thenReturn(users);

		when(responseBuilder.createResponse(any(), any(), anyString(), eq(users))).thenReturn(response);

		Response result = userService.getAllUsers();

		assertNotNull(result);

		verify(userRepository).findAll();
	}

	@Test
	void getAllUsers_NoUsersFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findAll()).thenReturn(new ArrayList<>());

		assertThrows(ResourceNotFoundException.class, () -> userService.getAllUsers());
	}

	@Test
	void deleteUser_Success() {

		mockLoggedInUser();

		String username = "admin";

		User user = new User();
		user.setUsername(username);

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = userService.deleteUser(username);

		assertNotNull(result);

		verify(userRepository).delete(user);

		verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void deleteUser_UserNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser("admin"));
	}

	private void mockLoggedInUser() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, new ArrayList<>()));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void deleteUser_DeleteThrowsDatabaseException() {

		mockLoggedInUser();

		User user = new User();
		user.setUsername("admin");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		doThrow(new RuntimeException("DB Error")).when(userRepository).delete(user);

		assertThrows(DatabaseException.class, () -> userService.deleteUser("admin"));
	}

	@Test
	void login_UserNotFound() {

		LoginModel model = new LoginModel();
		model.setUsername("admin");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.login(model));
	}

	@Test
	void login_InvalidPassword() {

		LoginModel model = new LoginModel();
		model.setUsername("admin");
		model.setPassword("123");

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encoded");
		user.setStatus(true);

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches("123", "encoded")).thenReturn(false);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = userService.login(model);

		assertNotNull(result);
	}

	@Test
	void login_UserInactive() {

		LoginModel model = new LoginModel();
		model.setUsername("admin");
		model.setPassword("123");

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encoded");
		user.setStatus(false);

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = userService.login(model);

		assertNotNull(result);
	}

	@Test
	void login_ProjectNotFound() {

		LoginModel model = new LoginModel();
		model.setUsername("admin");
		model.setPassword("123");

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encoded");
		user.setStatus(true);
		user.setProjectIds(List.of("P1"));

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches("123", "encoded")).thenReturn(true);

		when(projectRepository.findById("P1")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.login(model));
	}

	@Test
	void updateUser_NoChangesFound() {

		User user = new User();
		user.setUsername("admin");
		user.setFullname("Admin");
		user.setRole("USER");
		user.setStatus(true);

		UserUpdateModel model = new UserUpdateModel();
		model.setUsername("admin");
		model.setFullname("Admin");
		model.setRole("USER");
		model.setActive(true);

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		assertThrows(ValidationException.class, () -> userService.updateUser(model));
	}

	@Test
	void updateUser_ProjectNotFound() {

		User user = new User();
		user.setUsername("admin");

		UserUpdateModel model = new UserUpdateModel();
		model.setUsername("admin");
		model.setProjectIds(List.of("P1"));

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(projectRepository.findById("P1")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(model));
	}

	@Test
	void deleteUser_NullUsername() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		assertThrows(ValidationException.class, () -> userService.deleteUser(null));
	}

}
