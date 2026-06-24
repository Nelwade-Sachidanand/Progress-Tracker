package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.*;

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
import com.novillex.progresstracker.entity.*;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.repository.*;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.serviceImpl.ActivityUpdateRequestServiceImpl;
import com.novillex.progresstracker.util.UserContextUtil;

@ExtendWith(MockitoExtension.class)
class ActivityUpdateRequestServiceImplTest {

	@Mock
	private ActivityUpdateRequestRepository requestRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ApplicationContext context;

	@Mock
	private AuditService auditService;

	@Mock
	private AuditLogRepository auditLogRepository;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private ResponseBuilder responseBuilder;

	@InjectMocks
	private ActivityUpdateRequestServiceImpl service;

	@BeforeEach
    void setup() {
        when(context.getBean(ResponseBuilder.class))
                .thenReturn(responseBuilder);
    }

	private ActivityUpdateRequest getRequest() {

		Activity activity = new Activity();
		activity.setActivityName("Activity1");

		ActivityUpdateRequest req = new ActivityUpdateRequest();

		req.setId("REQ1");
		req.setStatus("PENDING");
		req.setProjectId("P001");
		req.setActivityName("Activity1");

		req.setPhaseName("Phase1");
		req.setMilestoneName("Milestone1");
		req.setTaskName("Task1");
		req.setSubTaskName("SubTask1");

		req.setOldActivity(activity);
		req.setNewActivity(activity);

		return req;
	}

	private Project getProject() {

		Activity activity = new Activity();
		activity.setActivityName("Activity1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(List.of(activity));

		Task task = new Task();
		task.setTaskName("Task1");
		task.setSubTasks(List.of(subtask));

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(List.of(task));

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");
		phase.setMilestones(List.of(milestone));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(List.of(phase));

		return project;
	}

	@Test
	void getPendingRequests_Success() {

		List<ActivityUpdateRequest> requests = List.of(getRequest());

		when(requestRepository.findByStatus("PENDING")).thenReturn(requests);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		Response response = service.getPendingRequests();

		assertNotNull(response);

		verify(requestRepository).findByStatus("PENDING");
	}

	@Test
	void approveRequest_Success() {

		ActivityUpdateRequest request = getRequest();

		Project project = getProject();

		Response response = new Response();

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.approveRequest("REQ1");

			assertNotNull(result);

			assertEquals("APPROVED", request.getStatus());

			verify(projectRepository).save(any());

			verify(requestRepository).save(any());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void rejectRequest_Success() {

		ActivityUpdateRequest request = getRequest();

		Response response = new Response();

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findById("REQ1")).thenReturn(Optional.of(request));

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.rejectRequest("REQ1", "Rejected");

			assertNotNull(result);

			assertEquals("REJECTED", request.getStatus());

			verify(requestRepository).save(any());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void approveSelectedRequests_Success() {

		ActivityUpdateRequest request = getRequest();

		Project project = getProject();

		Response response = new Response();

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(any())).thenReturn(List.of(request));

			when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.approveSelectedRequests(List.of("REQ1"));

			assertNotNull(result);

			verify(requestRepository).saveAll(any());

			verify(projectRepository).save(any());
		}
	}

	@Test
	void rejectSelectedRequests_Success() {

		ActivityUpdateRequest request = getRequest();

		Response response = new Response();

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(requestRepository.findAllById(any())).thenReturn(List.of(request));

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.rejectSelectedRequests(List.of("REQ1"), "Bulk Reject");

			assertNotNull(result);

			verify(requestRepository).saveAll(any());
		}
	}

	@Test
	void rollbackActivity_Success() {

		AuditLog audit = new AuditLog();

		audit.setProjectName("Demo Project");

		Activity activity = new Activity();

		activity.setActivityName("Activity1");

		Project project = getProject();

		Response response = new Response();

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			when(auditLogRepository.findById("AUD1")).thenReturn(Optional.of(audit));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			when(objectMapper.convertValue(any(), eq(Activity.class))).thenReturn(activity);

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = service.rollbackActivity("AUD1");

			assertNotNull(result);

			verify(projectRepository).save(any());

			verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void getAllRequests_Success() {

		List<ActivityUpdateRequest> requests = List.of(getRequest());

		Response response = new Response();

		when(requestRepository.findAll()).thenReturn(requests);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = service.getAllRequests();

		assertNotNull(result);

		verify(requestRepository).findAll();
	}

}