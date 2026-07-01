package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.ActivityUpdateRequest;
import com.novillex.progresstracker.entity.AuditLog;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.AuditLogRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.serviceImpl.ActivityUpdateRequestServiceImpl;
import com.novillex.progresstracker.util.UserContextUtil;

@ExtendWith(MockitoExtension.class)
class ActivityUpdateRequestServiceImplTest {

	@Mock
	private ActivityUpdateRequestRepository requestRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private AuditLogRepository auditLogRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private AuditService auditService;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private ApplicationContext context;

	@Mock
	private ResponseBuilder responseBuilder;

	@InjectMocks
	private ActivityUpdateRequestServiceImpl service;

	@BeforeEach
	void setup() {

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
	}

	private Activity buildActivity() {

		Activity activity = new Activity();

		activity.setActivityName("Activity1");
		activity.setProgress(20);
		activity.setExecutionStatus("In Progress");
		activity.setScheduleHealth("On Track");

		return activity;
	}

	private Activity buildUpdatedActivity() {

		Activity activity = new Activity();

		activity.setActivityName("Activity1");
		activity.setProgress(100);
		activity.setExecutionStatus("Completed");
		activity.setScheduleHealth("Completed");

		return activity;
	}

	private ActivityUpdateRequest buildRequest() {

		Activity oldActivity = buildActivity();

		Activity newActivity = buildUpdatedActivity();

		ActivityUpdateRequest request = new ActivityUpdateRequest();

		request.setId("REQ1");
		request.setProjectId("P001");

		request.setStatus("PENDING");

		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		request.setRequestedByUserId("USER1");
		request.setRequestedBy("developer");

		request.setOldActivity(oldActivity);
		request.setNewActivity(newActivity);

		return request;
	}

	private Project buildProject() {

		Activity activity = buildActivity();

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(new ArrayList<>(List.of(activity)));

		Task task = new Task();
		task.setTaskName("Task1");
		task.setSubTasks(new ArrayList<>(List.of(subtask)));

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(new ArrayList<>(List.of(task)));

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>(List.of(milestone)));

		Project project = new Project();

		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		return project;
	}

	private AuditLog buildAuditLog() {

		AuditLog auditLog = new AuditLog();
		auditLog.setProjectName("Demo Project");

		return auditLog;
	}


	@Test
	void getPendingRequests_Success() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		List<ActivityUpdateRequest> requests = List.of(buildRequest());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(requestRepository.findByStatus("PENDING")).thenReturn(requests);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = service.getPendingRequests();

		assertNotNull(result);

		verify(requestRepository).findByStatus("PENDING");
	}

	@Test
	void getPendingRequests_EmptyList() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(requestRepository.findByStatus("PENDING")).thenReturn(List.of());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = service.getPendingRequests();

		assertNotNull(result);

		verify(requestRepository).findByStatus("PENDING");
	}

	@Test
	void getPendingRequests_RepositoryThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(requestRepository.findByStatus("PENDING")).thenThrow(new RuntimeException("Database unavailable"));

		assertThrows(RuntimeException.class, () -> service.getPendingRequests());

		verify(requestRepository).findByStatus("PENDING");

		verify(responseBuilder, never()).createResponse(any(), any(), anyString(), any());
	}

	@Test
	void approveRequest_Success() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.approveRequest("REQ1");

			assertNotNull(result);

			assertEquals("APPROVED", request.getStatus());

			assertEquals("admin", request.getApprovedBy());

			assertNotNull(request.getApprovedAt());

			Activity updatedActivity = project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks()
					.get(0).getActivities().get(0);

			assertEquals(request.getNewActivity().getProgress(), updatedActivity.getProgress());

			assertEquals(request.getNewActivity().getExecutionStatus(), updatedActivity.getExecutionStatus());

			assertEquals(request.getNewActivity().getScheduleHealth(), updatedActivity.getScheduleHealth());

			verify(requestRepository).findById("REQ1");

			verify(projectRepository).findById("P001");

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService).createNotification(eq("Activity Update Approved"), contains("Activity1"),
					eq("ACTIVITY_APPROVED"), eq("REQ1"), eq("/tasks"), eq(request.getRequestedByUserId()));

			verify(auditService).saveAuditLog(any(), any(), eq("Activity1"), eq("Demo Project"),
					eq(request.getOldActivity()), eq(request.getNewActivity()), eq("admin"));
		}
	}

	@Test
	void approveRequest_RequestNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.empty());

			assertThrows(ResourceNotFoundException.class, () -> service.approveRequest("REQ1"));

			verify(requestRepository).findById("REQ1");

			verify(projectRepository, never()).findById(anyString());

			verify(projectRepository, never()).save(any(Project.class));

			verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));

			verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveRequest_ProjectNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.empty());

			assertThrows(ResourceNotFoundException.class, () -> service.approveRequest("REQ1"));

			verify(requestRepository).findById("REQ1");

			verify(projectRepository).findById("P001");

			verify(projectRepository, never()).save(any(Project.class));

			verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));

			verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveRequest_ActivityNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			assertThrows(ResourceNotFoundException.class, () -> service.approveRequest("REQ1"));

			verify(requestRepository).findById("REQ1");

			verify(projectRepository).findById("P001");

			verify(projectRepository, never()).save(any(Project.class));

			verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));

			verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveRequest_ProjectSaveThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenThrow(new RuntimeException("Database Error"));

			assertThrows(RuntimeException.class, () -> service.approveRequest("REQ1"));

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));

			verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveRequest_RequestSaveThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			when(requestRepository.save(any(ActivityUpdateRequest.class)))
					.thenThrow(new RuntimeException("Database Error"));

			assertThrows(RuntimeException.class, () -> service.approveRequest("REQ1"));

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService, never()).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveRequest_NotificationThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doThrow(new RuntimeException("Notification Error")).when(notificationService)
					.createNotification(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

			assertThrows(RuntimeException.class, () -> service.approveRequest("REQ1"));

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveRequest_AuditServiceThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			doThrow(new RuntimeException("Audit Error")).when(auditService).saveAuditLog(any(), any(), any(), any(),
					any(), any(), any());

			assertThrows(RuntimeException.class, () -> service.approveRequest("REQ1"));

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveRequest_ResponseBuilderCalledSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), eq("Request approved successfully"), eq(request)))
					.thenReturn(response);

			Response result = service.approveRequest("REQ1");

			assertNotNull(result);

			verify(responseBuilder).createResponse(any(), any(), eq("Request approved successfully"), eq(request));
		}
	}

	@Test
	void approveRequest_VerifyApprovalDetails() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			service.approveRequest("REQ1");

			assertEquals("APPROVED", request.getStatus());

			assertEquals("admin", request.getApprovedBy());

			assertNotNull(request.getApprovedAt());

			assertEquals(request.getNewActivity().getProgress(), project.getPhases().get(0).getMilestones().get(0)
					.getTasks().get(0).getSubTasks().get(0).getActivities().get(0).getProgress());
		}
	}

	@Test
	void rejectRequest_Success() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.rejectRequest("REQ1", "Rejected");

			assertNotNull(result);

			assertEquals("REJECTED", request.getStatus());

			assertEquals("Rejected", request.getRejectionReason());

			assertEquals("admin", request.getApprovedBy());

			assertNotNull(request.getApprovedAt());

			verify(requestRepository).findById("REQ1");

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService).createNotification(eq("Activity Update Rejected"), contains("Activity1"),
					eq("ACTIVITY_REJECTED"), eq("REQ1"), eq("/tasks"), eq(request.getRequestedByUserId()));

			verify(auditService).saveAuditLog(any(), any(), eq("Activity1"), isNull(), eq(request.getOldActivity()),
					eq(request.getNewActivity()), eq("admin"));
		}
	}

	@Test
	void rejectRequest_RequestNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.empty());

			assertThrows(ResourceNotFoundException.class, () -> service.rejectRequest("REQ1", "Rejected"));

			verify(requestRepository).findById("REQ1");

			verify(requestRepository, never()).save(any());

			verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rejectRequest_SaveThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(requestRepository.save(any(ActivityUpdateRequest.class)))
					.thenThrow(new RuntimeException("Database Error"));

			assertThrows(RuntimeException.class, () -> service.rejectRequest("REQ1", "Rejected"));

			verify(requestRepository).findById("REQ1");

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rejectRequest_NotificationThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doThrow(new RuntimeException("Notification Error")).when(notificationService)
					.createNotification(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

			assertThrows(RuntimeException.class, () -> service.rejectRequest("REQ1", "Rejected"));

			verify(requestRepository).findById("REQ1");

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rejectRequest_AuditServiceThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			doThrow(new RuntimeException("Audit Error")).when(auditService).saveAuditLog(any(), any(), any(), any(),
					any(), any(), any());

			assertThrows(RuntimeException.class, () -> service.rejectRequest("REQ1", "Rejected"));

			verify(requestRepository).findById("REQ1");

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rejectRequest_VerifyApprovalDetails() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(requestRepository.save(any(ActivityUpdateRequest.class))).thenReturn(request);

			doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(), anyString(),
					anyString(), anyString());

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			service.rejectRequest("REQ1", "Rejected");

			assertEquals("REJECTED", request.getStatus());

			assertEquals("Rejected", request.getRejectionReason());

			assertEquals("admin", request.getApprovedBy());

			assertNotNull(request.getApprovedAt());

			verify(requestRepository).save(any(ActivityUpdateRequest.class));

			verify(notificationService).createNotification(eq("Activity Update Rejected"),
					contains(request.getActivityName()), eq("ACTIVITY_REJECTED"), eq(request.getId()), eq("/tasks"),
					eq(request.getRequestedByUserId()));

			verify(auditService).saveAuditLog(any(), any(), eq(request.getActivityName()), isNull(),
					eq(request.getOldActivity()), eq(request.getNewActivity()), eq("admin"));
		}
	}

	@Test
	void approveSelectedRequests_Success() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			Response result = service.approveSelectedRequests(List.of("REQ1"));

			assertNotNull(result);

			assertEquals("APPROVED", request.getStatus());

			assertEquals("admin", request.getApprovedBy());

			assertNotNull(request.getApprovedAt());

			verify(requestRepository).findAllById(List.of("REQ1"));

			verify(projectRepository).findById("P001");

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository).saveAll(any());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), eq("admin"));
		}
	}

	@Test
	void approveSelectedRequests_NoRequestsFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(new ArrayList<>());

			assertThrows(ResourceNotFoundException.class, () -> service.approveSelectedRequests(List.of("REQ1")));

			verify(requestRepository).findAllById(List.of("REQ1"));

			verify(projectRepository, never()).findById(anyString());

			verify(projectRepository, never()).save(any());

			verify(requestRepository, never()).saveAll(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveSelectedRequests_ProjectNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.empty());

			assertThrows(ResourceNotFoundException.class, () -> service.approveSelectedRequests(List.of("REQ1")));

			verify(requestRepository).findAllById(List.of("REQ1"));

			verify(projectRepository).findById("P001");

			verify(projectRepository, never()).save(any());

			verify(requestRepository, never()).saveAll(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveSelectedRequests_ActivityNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			assertThrows(ResourceNotFoundException.class, () -> service.approveSelectedRequests(List.of("REQ1")));

			verify(requestRepository).findAllById(List.of("REQ1"));

			verify(projectRepository).findById("P001");

			verify(projectRepository, never()).save(any());

			verify(requestRepository, never()).saveAll(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveSelectedRequests_ProjectSaveThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenThrow(new RuntimeException("Database Error"));

			assertThrows(RuntimeException.class, () -> service.approveSelectedRequests(List.of("REQ1")));

			verify(requestRepository).findAllById(List.of("REQ1"));

			verify(projectRepository).findById("P001");

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository, never()).saveAll(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveSelectedRequests_SaveAllThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			doThrow(new RuntimeException("Database Error")).when(requestRepository).saveAll(any());

			assertThrows(RuntimeException.class, () -> service.approveSelectedRequests(List.of("REQ1")));

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository).saveAll(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveSelectedRequests_AuditServiceThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			when(requestRepository.saveAll(any())).thenReturn(List.of(request));

			doThrow(new RuntimeException("Audit Error")).when(auditService).saveAuditLog(any(), any(), any(), any(),
					any(), any(), any());

			assertThrows(RuntimeException.class, () -> service.approveSelectedRequests(List.of("REQ1")));

			verify(projectRepository).save(any(Project.class));

			verify(requestRepository).saveAll(any());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), eq("admin"));
		}
	}

	@Test
	void rejectSelectedRequests_Success() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(requestRepository.saveAll(any())).thenReturn(List.of(request));

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.rejectSelectedRequests(List.of("REQ1"), "Rejected");

			assertNotNull(result);

			assertEquals("REJECTED", request.getStatus());

			assertEquals("Rejected", request.getRejectionReason());

			assertEquals("admin", request.getApprovedBy());

			assertNotNull(request.getApprovedAt());

			verify(requestRepository).findAllById(List.of("REQ1"));

			verify(requestRepository).saveAll(any());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), eq("admin"));
		}
	}

	@Test
	void rejectSelectedRequests_NoRequestsFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(new ArrayList<>());

			assertThrows(ResourceNotFoundException.class,
					() -> service.rejectSelectedRequests(List.of("REQ1"), "Rejected"));

			verify(requestRepository).findAllById(List.of("REQ1"));

			verify(requestRepository, never()).saveAll(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rejectSelectedRequests_SaveAllThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			doThrow(new RuntimeException("Database Error")).when(requestRepository).saveAll(any());

			assertThrows(RuntimeException.class, () -> service.rejectSelectedRequests(List.of("REQ1"), "Rejected"));

			verify(requestRepository).findAllById(List.of("REQ1"));

			verify(requestRepository).saveAll(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rejectSelectedRequests_AuditServiceThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(requestRepository.saveAll(any())).thenReturn(List.of(request));

			doThrow(new RuntimeException("Audit Error")).when(auditService).saveAuditLog(any(), any(), any(), any(),
					any(), any(), any());

			assertThrows(RuntimeException.class, () -> service.rejectSelectedRequests(List.of("REQ1"), "Rejected"));

			verify(requestRepository).saveAll(any());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), eq("admin"));
		}
	}

	@Test
	void rejectSelectedRequests_VerifyRejectedStatus() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(requestRepository.saveAll(any())).thenReturn(List.of(request));

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			service.rejectSelectedRequests(List.of("REQ1"), "Rejected");

			assertEquals("REJECTED", request.getStatus());

			assertEquals("admin", request.getApprovedBy());

			assertNotNull(request.getApprovedAt());
		}
	}

	@Test
	void rejectSelectedRequests_VerifyRejectionReason() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ActivityUpdateRequest request = buildRequest();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(List.of("REQ1"))).thenReturn(List.of(request));

			when(requestRepository.saveAll(any())).thenReturn(List.of(request));

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			service.rejectSelectedRequests(List.of("REQ1"), "Rejected");

			assertEquals("Rejected", request.getRejectionReason());

			verify(requestRepository).saveAll(any());

			verify(auditService).saveAuditLog(any(), any(), contains("Bulk Rejection"), isNull(), any(), any(),
					eq("admin"));
		}
	}

	@Test
	void rollbackActivity_Success() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		AuditLog auditLog = buildAuditLog();

		Project project = buildProject();

		Activity activity = buildActivity();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(auditLogRepository.findById("AUD1")).thenReturn(Optional.of(auditLog));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			when(objectMapper.convertValue(any(), eq(Activity.class))).thenReturn(activity);

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.rollbackActivity("AUD1");

			assertNotNull(result);

			verify(auditLogRepository).findById("AUD1");

			verify(projectRepository).findByProjectName("Demo Project");

			verify(projectRepository).save(any(Project.class));

			verify(auditService).saveAuditLog(any(), any(), eq("Activity1"), eq("Demo Project"), isNull(), eq(activity),
					eq("admin"));
		}
	}

	@Test
	void rollbackActivity_AuditLogNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(auditLogRepository.findById("AUD1")).thenReturn(Optional.empty());

			assertThrows(ResourceNotFoundException.class, () -> service.rollbackActivity("AUD1"));

			verify(auditLogRepository).findById("AUD1");

			verify(projectRepository, never()).findByProjectName(anyString());

			verify(projectRepository, never()).save(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rollbackActivity_ProjectNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		AuditLog auditLog = buildAuditLog();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(auditLogRepository.findById("AUD1")).thenReturn(Optional.of(auditLog));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.empty());

			assertThrows(ResourceNotFoundException.class, () -> service.rollbackActivity("AUD1"));

			verify(auditLogRepository).findById("AUD1");

			verify(projectRepository).findByProjectName("Demo Project");

			verify(projectRepository, never()).save(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rollbackActivity_ActivityNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		AuditLog auditLog = buildAuditLog();

		Project project = new Project();
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		Activity activity = buildActivity();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(auditLogRepository.findById("AUD1")).thenReturn(Optional.of(auditLog));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			when(objectMapper.convertValue(any(), eq(Activity.class))).thenReturn(activity);

			assertThrows(ResourceNotFoundException.class, () -> service.rollbackActivity("AUD1"));

			verify(projectRepository, never()).save(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rollbackActivity_ProjectSaveThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		AuditLog auditLog = buildAuditLog();

		Project project = buildProject();

		Activity activity = buildActivity();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(auditLogRepository.findById("AUD1")).thenReturn(Optional.of(auditLog));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			when(objectMapper.convertValue(any(), eq(Activity.class))).thenReturn(activity);

			when(projectRepository.save(any(Project.class))).thenThrow(new RuntimeException("Database Error"));

			assertThrows(RuntimeException.class, () -> service.rollbackActivity("AUD1"));

			verify(projectRepository).save(any(Project.class));

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rollbackActivity_AuditServiceThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		AuditLog auditLog = buildAuditLog();

		Project project = buildProject();

		Activity activity = buildActivity();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(auditLogRepository.findById("AUD1")).thenReturn(Optional.of(auditLog));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			when(objectMapper.convertValue(any(), eq(Activity.class))).thenReturn(activity);

			when(projectRepository.save(any(Project.class))).thenReturn(project);

			doThrow(new RuntimeException("Audit Error")).when(auditService).saveAuditLog(any(), any(), any(), any(),
					any(), any(), any());

			assertThrows(RuntimeException.class, () -> service.rollbackActivity("AUD1"));

			verify(projectRepository).save(any(Project.class));

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), eq("admin"));
		}
	}

	@Test
	void rollbackActivity_ObjectMapperThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		AuditLog auditLog = buildAuditLog();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(auditLogRepository.findById("AUD1")).thenReturn(Optional.of(auditLog));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			when(objectMapper.convertValue(any(), eq(Activity.class)))
					.thenThrow(new RuntimeException("ObjectMapper Error"));

			assertThrows(RuntimeException.class, () -> service.rollbackActivity("AUD1"));

			verify(projectRepository, never()).save(any());

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void getAllRequests_Success() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		List<ActivityUpdateRequest> requests = List.of(buildRequest());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(requestRepository.findAll()).thenReturn(requests);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = service.getAllRequests();

		assertNotNull(result);

		verify(requestRepository).findAll();
	}

	@Test
	void getAllRequests_EmptyList() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(requestRepository.findAll()).thenReturn(new ArrayList<>());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = service.getAllRequests();

		assertNotNull(result);

		verify(requestRepository).findAll();
	}

	@Test
	void getAllRequests_RepositoryThrowsException() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(requestRepository.findAll()).thenThrow(new RuntimeException("Database Error"));

		assertThrows(RuntimeException.class, () -> service.getAllRequests());

		verify(requestRepository).findAll();
	}

}
