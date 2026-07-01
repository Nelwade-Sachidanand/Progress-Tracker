package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.ActivityUpdateRequest;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.serviceImpl.RollbackServiceImpl;
import com.novillex.progresstracker.util.UserContextUtil;

@ExtendWith(MockitoExtension.class)
class RollbackServiceImplTest {

	@Mock
	private ActivityUpdateRequestRepository requestRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private AuditService auditService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	private ResponseBuilder responseBuilder;

	@InjectMocks
	private RollbackServiceImpl rollbackService;

	@BeforeEach
	void setUp() {

	    when(applicationContext.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);
	}

	@Test
	void rollbackRequest_WhenRequestNotFound_ShouldThrowResourceNotFoundException() {

		// Arrange
		String requestId = "REQ001";

		when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

		// Act & Assert
		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> rollbackService.rollbackRequest(requestId, "password", "Rollback"));

		assertEquals(ErrorCode.REQUEST_NOT_FOUND, exception.getErrorCode());

		verify(requestRepository).findById(requestId);
		verify(projectRepository, never()).findById(anyString());
		verify(projectRepository, never()).save(any());
	}

	@Test
	void rollbackRequest_WhenRequestStatusIsPending_ShouldThrowValidationException() {

		// Arrange
		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId("REQ001");
		request.setStatus("PENDING");

		when(requestRepository.findById("REQ001")).thenReturn(Optional.of(request));

		// Act & Assert
		ValidationException exception = assertThrows(ValidationException.class,
				() -> rollbackService.rollbackRequest("REQ001", "password", "Rollback"));

		assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

		verify(requestRepository).findById("REQ001");
		verify(userRepository, never()).findByUsername(anyString());
	}

	@Test
	void rollbackRequest_WhenPasswordIsInvalid_ShouldThrowValidationException() {

		// Arrange
		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId("REQ001");
		request.setProjectId("PROJECT001");
		request.setStatus("APPROVED");

		User user = new User();
		user.setUsername("admin");
		user.setPassword("$2a$10$encodedPassword");

		when(requestRepository.findById("REQ001")).thenReturn(Optional.of(request));

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

			when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);

			// Act & Assert
			ValidationException exception = assertThrows(ValidationException.class,
					() -> rollbackService.rollbackRequest("REQ001", "wrongPassword", "Rollback"));

			assertEquals(ErrorCode.INVALID_PASSWORD, exception.getErrorCode());

			verify(userRepository).findByUsername("admin");
			verify(projectRepository, never()).findById(anyString());
		}
	}

	@Test
	void rollbackRequest_WhenCurrentUserNotFound_ShouldThrowResourceNotFoundException() {

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId("REQ001");
		request.setProjectId("PROJECT001");
		request.setStatus("APPROVED");

		when(requestRepository.findById("REQ001")).thenReturn(Optional.of(request));

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

			ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
					() -> rollbackService.rollbackRequest("REQ001", "password", "Rollback"));

			assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());

			verify(userRepository).findByUsername("admin");
			verify(projectRepository, never()).findById(anyString());
		}
	}

	@Test
	void rollbackRequest_WhenProjectNotFound_ShouldThrowResourceNotFoundException() {

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId("REQ001");
		request.setProjectId("PROJECT001");
		request.setStatus("APPROVED");

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		when(requestRepository.findById("REQ001")).thenReturn(Optional.of(request));

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

			when(passwordEncoder.matches("password", user.getPassword())).thenReturn(true);

			when(projectRepository.findById("PROJECT001")).thenReturn(Optional.empty());

			ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
					() -> rollbackService.rollbackRequest("REQ001", "password", "Rollback"));

			assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.getErrorCode());

			verify(projectRepository).findById("PROJECT001");
			verify(projectRepository, never()).save(any());
		}
	}

	@Test
	void rollbackRequest_WhenActivityNotFound_ShouldThrowResourceNotFoundException() {

		// Arrange
		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId("REQ001");
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setStatus("APPROVED");

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		// Project with empty phases (avoids NullPointerException)
		Project project = new Project();
		project.setId("PROJECT001");
		project.setPhases(new ArrayList<>());

		when(requestRepository.findById("REQ001")).thenReturn(Optional.of(request));

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

			when(passwordEncoder.matches("password", user.getPassword())).thenReturn(true);

			when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

			// Act & Assert
			ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
					() -> rollbackService.rollbackRequest("REQ001", "password", "Rollback"));

			assertEquals(ErrorCode.ACTIVITY_NOT_FOUND, exception.getErrorCode());

			verify(projectRepository).findById("PROJECT001");
			verify(projectRepository, never()).save(any(Project.class));
			verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));
			verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());
			verify(auditService, never()).saveAuditLog(any(), any(), anyString(), anyString(), any(), any(),
					anyString());
		}
	}

	@Test
	void rollbackRequest_WhenRollbackIsSuccessful_ShouldReturnSuccessResponse() {

		// Arrange
		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback for testing";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setRequestedByUserId("USER001");
		request.setStatus("APPROVED");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

			when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

			when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

			when(applicationContext.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(responseBuilder.createResponse(anyString(), anyString(), anyString(), any()))
					.thenReturn(new Response());

			// Act
			Response response = rollbackService.rollbackRequest(requestId, password, reason);

			// Assert
			assertNotNull(response);

			assertEquals("ROLLED_BACK", request.getStatus());
			assertEquals(reason, request.getRollbackReason());
			assertEquals("admin", request.getRolledBackBy());
			assertNotNull(request.getRolledBackAt());

			verify(projectRepository).save(project);
			verify(requestRepository).save(request);

			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
					anyString());
		}
	}

	@Test
	void rollbackRequest_WhenRollbackIsSuccessful_ShouldUpdateStatusToRolledBack() {

		// Arrange
		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback for testing";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setRequestedByUserId("USER001");
		request.setStatus("APPROVED");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		// Mock dependencies
		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

		when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

		when(applicationContext.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(responseBuilder.createResponse(anyString(), anyString(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			// Act
			Response response = rollbackService.rollbackRequest(requestId, password, reason);

			// Assert
			assertNotNull(response);

			assertEquals("ROLLED_BACK", request.getStatus());

			verify(projectRepository).save(project);
			verify(requestRepository).save(request);

			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
					anyString());
		}
	}

	@Test
	void rollbackRequest_WhenRollbackIsSuccessful_ShouldUpdateRollbackDetails() {

		// Arrange
		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback for testing";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setRequestedByUserId("USER001");
		request.setStatus("APPROVED");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		// Mock Repository Calls
		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

		when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

		when(applicationContext.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(responseBuilder.createResponse(anyString(), anyString(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			// Act
			Response response = rollbackService.rollbackRequest(requestId, password, reason);

			// Assert
			assertNotNull(response);

			assertEquals("ROLLED_BACK", request.getStatus());
			assertEquals(reason, request.getRollbackReason());
			assertEquals("admin", request.getRolledBackBy());
			assertNotNull(request.getRolledBackAt());

			verify(projectRepository).save(project);
			verify(requestRepository).save(request);

			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
					anyString());
		}
	}

	@Test
	void rollbackRequest_WhenRollbackIsSuccessful_ShouldCreateNotificationAndAuditLog() {

		
		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback for testing";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setRequestedByUserId("USER001");
		request.setStatus("APPROVED");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		
		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

		when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

		when(applicationContext.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(responseBuilder.createResponse(anyString(), anyString(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			
			rollbackService.rollbackRequest(requestId, password, reason);

			
			verify(notificationService).createNotification("Activity Rolled Back",
					"Changes for activity Activity-1 were rolled back by admin", "ACTIVITY_ROLLBACK", requestId,
					"/tasks", "USER001");

			verify(auditService).saveAuditLog(AuditAction.ROLLBACK_ACTIVITY_UPDATE, AuditEntity.ACTIVITY, "Activity-1",
					"Demo Project", newActivity, oldActivity, "admin");
		}
	}

	@Test
	void rollbackRequest_WhenRequestStatusIsRejected_ShouldRollbackSuccessfully() {

		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback Rejected Request";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setRequestedByUserId("USER001");
		request.setStatus("REJECTED");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
		when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));
		when(applicationContext.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
		when(responseBuilder.createResponse(anyString(), anyString(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			Response response = rollbackService.rollbackRequest(requestId, password, reason);

			assertNotNull(response);
			assertEquals("ROLLED_BACK", request.getStatus());

			verify(projectRepository).save(project);
			verify(requestRepository).save(request);
		}
	}

	@Test
	void rollbackRequest_WhenRequestStatusIsRolledBack_ShouldThrowValidationException() {

		String requestId = "REQ001";

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setStatus("ROLLED_BACK");

		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		// Act & Assert
		ValidationException exception = assertThrows(ValidationException.class,
				() -> rollbackService.rollbackRequest(requestId, "password", "Rollback"));

		assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

		verify(requestRepository).findById(requestId);

		verify(userRepository, never()).findByUsername(anyString());
		verify(projectRepository, never()).findById(anyString());
		verify(projectRepository, never()).save(any(Project.class));
		verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));
		verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
				anyString(), anyString());
		verify(auditService, never()).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
				anyString());
	}

	@Test
	void rollbackRequest_WhenProjectRepositorySaveFails_ShouldThrowRuntimeException() {

		
		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setStatus("APPROVED");
		request.setRequestedByUserId("USER001");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

		when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

		doThrow(new RuntimeException("Database Error")).when(projectRepository).save(any(Project.class));

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			RuntimeException exception = assertThrows(RuntimeException.class,
					() -> rollbackService.rollbackRequest(requestId, password, reason));

			assertEquals("Database Error", exception.getMessage());

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));

			verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
					anyString());
		}
	}

	@Test
	void rollbackRequest_WhenRequestRepositorySaveFails_ShouldThrowRuntimeException() {

		
		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setRequestedByUserId("USER001");
		request.setStatus("APPROVED");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

		when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

		when(projectRepository.save(any(Project.class))).thenReturn(project);

		doThrow(new RuntimeException("Unable to save request")).when(requestRepository)
				.save(any(ActivityUpdateRequest.class));

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			RuntimeException exception = assertThrows(RuntimeException.class,
					() -> rollbackService.rollbackRequest(requestId, password, reason));

			assertEquals("Unable to save request", exception.getMessage());

			verify(projectRepository).save(any(Project.class));
			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
					anyString());
		}
	}

	@Test
	void rollbackRequest_WhenNotificationServiceThrowsException_ShouldThrowRuntimeException() {

		
		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setStatus("APPROVED");
		request.setRequestedByUserId("USER001");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

		when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

		when(projectRepository.save(any(Project.class))).thenReturn(project);

		when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

		doThrow(new RuntimeException("Notification Error")).when(notificationService).createNotification(anyString(),
				anyString(), anyString(), anyString(), anyString(), anyString());

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			RuntimeException exception = assertThrows(RuntimeException.class,
					() -> rollbackService.rollbackRequest(requestId, password, reason));

			assertEquals("Notification Error", exception.getMessage());

			verify(projectRepository).save(any(Project.class));
			verify(requestRepository).save(any(ActivityUpdateRequest.class));
			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
					anyString());
		}
	}

	@Test
	void rollbackRequest_WhenAuditServiceThrowsException_ShouldThrowRuntimeException() {

		
		String requestId = "REQ001";
		String password = "password";
		String reason = "Rollback";

		Activity oldActivity = new Activity();
		oldActivity.setActivityName("Activity-1");

		Activity newActivity = new Activity();
		newActivity.setActivityName("Activity-1");

		ActivityUpdateRequest request = new ActivityUpdateRequest();
		request.setId(requestId);
		request.setProjectId("PROJECT001");
		request.setPhaseName("Phase-1");
		request.setMilestoneName("Milestone-1");
		request.setTaskName("Task-1");
		request.setSubTaskName("SubTask-1");
		request.setActivityName("Activity-1");
		request.setRequestedByUserId("USER001");
		request.setStatus("APPROVED");
		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		User user = new User();
		user.setUsername("admin");
		user.setPassword("encodedPassword");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(new ArrayList<>());
		subtask.getActivities().add(activity);

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(new ArrayList<>());
		task.getSubTasks().add(subtask);

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(new ArrayList<>());
		milestone.getTasks().add(task);

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(new ArrayList<>());
		phase.getMilestones().add(milestone);

		Project project = new Project();
		project.setId("PROJECT001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());
		project.getPhases().add(phase);

		when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

		when(projectRepository.findById("PROJECT001")).thenReturn(Optional.of(project));

		when(projectRepository.save(any(Project.class))).thenReturn(project);

		when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

		doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
				anyString(), anyString());

		doThrow(new RuntimeException("Audit Error")).when(auditService).saveAuditLog(anyString(), anyString(),
				anyString(), anyString(), any(), any(), anyString());

		try (MockedStatic<UserContextUtil> mockedStatic = Mockito.mockStatic(UserContextUtil.class)) {

			mockedStatic.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			RuntimeException exception = assertThrows(RuntimeException.class,
					() -> rollbackService.rollbackRequest(requestId, password, reason));

			assertEquals("Audit Error", exception.getMessage());

			verify(projectRepository).save(any(Project.class));
			verify(requestRepository).save(any(ActivityUpdateRequest.class));
			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
					anyString());
		}
	}
	
	
	
}
