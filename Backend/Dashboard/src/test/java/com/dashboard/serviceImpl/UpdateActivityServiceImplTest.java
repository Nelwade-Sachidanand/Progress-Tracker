package com.dashboard.serviceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.ActivityUpdateRequestModel;
import com.novillex.progresstracker.model.AddRemarkModel;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.serviceImpl.UpdateActivityServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UpdateActivityServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ActivityUpdateRequestRepository requestRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private ApplicationContext context;

	@Mock
	private ResponseBuilder responseBuilder;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private UpdateActivityServiceImpl updateActivityService;

	private ActivityUpdateRequestModel buildActivityUpdateRequestModel() {

		ActivityUpdateRequestModel request = new ActivityUpdateRequestModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		request.setEstimatedPeriodWeek(5.0);
		request.setProgress(80);

		request.setChangeReason("Updating progress");

		return request;
	}

	private Project buildProject() {

		Activity activity = new Activity();

		activity.setActivityName("Activity1");
		activity.setEstimatedPeriodWeek(2.0);
		activity.setProgress(50);

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

	@Test
	void updateActivityRequest_Success() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = buildProject();

		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.empty());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(),
				nullable(String.class), anyString(), isNull());

		doNothing().when(auditService).saveAuditLog(any(), any(), anyString(), anyString(), any(), any(), anyString());

		Response result = updateActivityService.updateActivityRequest(request);

		assertNotNull(result);

		verify(projectRepository).findById("P001");

		verify(requestRepository).save(any(ActivityUpdateRequest.class));

		verify(notificationService).createNotification(eq("Activity Update Requested"),
				eq("admin requested update for activity Activity1"), eq("ACTIVITY_UPDATE"), isNull(),
				eq("/authorization"), isNull());
		verify(auditService).saveAuditLog(any(), any(), eq("Activity1"), eq("Demo Project"), any(), any(), eq("admin"));
	}

	@Test
	void updateActivityRequest_ProjectNotFound() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> updateActivityService.updateActivityRequest(request));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");

		verify(requestRepository, never()).save(any());
		verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_ActivityNotFound() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = new Project();

		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> updateActivityService.updateActivityRequest(request));

		assertEquals(ErrorCode.ACTIVITY_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");

		verify(requestRepository, never()).save(any());
		verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_NoChangesFound() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		request.setEstimatedPeriodWeek(2.0);
		request.setProgress(50);

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ValidationException exception = assertThrows(ValidationException.class,
				() -> updateActivityService.updateActivityRequest(request));

		assertEquals(ErrorCode.NO_CHANGES_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");

		verify(requestRepository, never()).save(any());
		verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_RequestAlreadyPending() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.of(new ActivityUpdateRequest()));

		ValidationException exception = assertThrows(ValidationException.class,
				() -> updateActivityService.updateActivityRequest(request));

		assertEquals(ErrorCode.REQUEST_ALREADY_PENDING, exception.getErrorCode());

		verify(requestRepository, never()).save(any());
	}

	@Test
	void updateActivityRequest_SaveRequestThrowsException() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.empty());

		doThrow(new RuntimeException("Database Error")).when(requestRepository).save(any(ActivityUpdateRequest.class));

		assertThrows(RuntimeException.class, () -> updateActivityService.updateActivityRequest(request));

		verify(requestRepository).save(any(ActivityUpdateRequest.class));
		verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), eq("PENDING"));
	}

	@Test
	void updateActivityRequest_NotificationThrowsException() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.empty());
		doThrow(new RuntimeException("Notification Error")).when(notificationService).createNotification(anyString(),
				anyString(), anyString(), nullable(String.class), anyString(), isNull());

		assertThrows(RuntimeException.class, () -> updateActivityService.updateActivityRequest(request));

		verify(requestRepository).save(any(ActivityUpdateRequest.class));
		verify(notificationService).createNotification(eq("Activity Update Requested"),
				eq("admin requested update for activity Activity1"), eq("ACTIVITY_UPDATE"), isNull(),
				eq("/authorization"), isNull());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_AuditServiceThrowsException() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = buildProject();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.empty());

		doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(),
				nullable(String.class), anyString(), isNull());

		doThrow(new RuntimeException("Audit Error")).when(auditService).saveAuditLog(any(), any(), any(), any(), any(),
				any(), any());

		assertThrows(RuntimeException.class, () -> updateActivityService.updateActivityRequest(request));

		verify(requestRepository).save(any(ActivityUpdateRequest.class));
		verify(notificationService).createNotification(eq("Activity Update Requested"),
				eq("admin requested update for activity Activity1"), eq("ACTIVITY_UPDATE"), isNull(),
				eq("/authorization"), isNull());

		verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	private AddRemarkModel buildAddRemarkModel() {

		AddRemarkModel model = new AddRemarkModel();

		model.setProjectId("P001");
		model.setProjectName("Demo Project");

		model.setPhaseName("Phase1");
		model.setMilestoneName("Milestone1");
		model.setTaskName("Task1");
		model.setSubTaskName("SubTask1");
		model.setActivityName("Activity1");

		model.setRemark("Activity completed successfully");

		return model;
	}

	@Test
	void addRemark_Success_WhenRemarkIsEmpty() {

		AddRemarkModel model = buildAddRemarkModel();

		Project project = buildProject();

		Response response = new Response();

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setRemark(null);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), isNull())).thenReturn(response);

		Response result = updateActivityService.addRemark(model);

		assertNotNull(result);

		assertEquals("Activity completed successfully", project.getPhases().get(0).getMilestones().get(0).getTasks()
				.get(0).getSubTasks().get(0).getActivities().get(0).getRemark());

		verify(projectRepository).save(project);

		verify(responseBuilder).createResponse(any(), any(), eq("Remark added successfully"), isNull());
	}

	@Test
	void addRemark_Success_WhenRemarkAlreadyExists() {

		AddRemarkModel model = buildAddRemarkModel();

		Project project = buildProject();

		Response response = new Response();

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setRemark("Old Remark");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), isNull())).thenReturn(response);

		Response result = updateActivityService.addRemark(model);

		assertNotNull(result);

		String remark = project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0)
				.getActivities().get(0).getRemark();

		assertTrue(remark.contains("Old Remark"));

		assertTrue(remark.contains("Activity completed successfully"));

		verify(projectRepository).save(project);
	}

	@Test
	void addRemark_ProjectNotFound() {

		AddRemarkModel model = buildAddRemarkModel();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> updateActivityService.addRemark(model));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");

		verify(projectRepository, never()).save(any(Project.class));
	}

	@Test
	void addRemark_ActivityNotFound() {

		AddRemarkModel model = buildAddRemarkModel();

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setActivityName("Another Activity");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> updateActivityService.addRemark(model));

		assertEquals(ErrorCode.ACTIVITY_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");

		verify(projectRepository, never()).save(any(Project.class));
	}

	@Test
	void addRemark_ProjectSaveThrowsException() {

		AddRemarkModel model = buildAddRemarkModel();

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setRemark(null);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		doThrow(new RuntimeException("Database Error")).when(projectRepository).save(any(Project.class));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> updateActivityService.addRemark(model));

		assertEquals("Database Error", exception.getMessage());

		verify(projectRepository).save(any(Project.class));

		verify(responseBuilder, never()).createResponse(any(), any(), anyString(), any());
	}

	@Test
	void addRemark_ResponseBuilderCalledSuccessfully() {

		AddRemarkModel model = buildAddRemarkModel();

		Project project = buildProject();

		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), eq("Remark added successfully"), isNull()))
				.thenReturn(response);

		Response result = updateActivityService.addRemark(model);

		assertSame(response, result);

		verify(responseBuilder).createResponse(any(), any(), eq("Remark added successfully"), isNull());

		verify(projectRepository).save(project);
	}

	@Test
	void addRemark_ShouldUpdateOnlyMatchingActivity() {

		AddRemarkModel model = buildAddRemarkModel();

		Project project = buildProject();

		Activity secondActivity = new Activity();

		secondActivity.setActivityName("Activity2");
		secondActivity.setRemark("Second Activity Remark");

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities()
				.add(secondActivity);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), isNull())).thenReturn(new Response());

		updateActivityService.addRemark(model);

		assertEquals("Activity completed successfully", project.getPhases().get(0).getMilestones().get(0).getTasks()
				.get(0).getSubTasks().get(0).getActivities().get(0).getRemark());

		assertEquals("Second Activity Remark", project.getPhases().get(0).getMilestones().get(0).getTasks().get(0)
				.getSubTasks().get(0).getActivities().get(1).getRemark());

		verify(projectRepository).save(project);
	}

	@Test
	void addRemark_ShouldPreserveExistingRemarkWithNewLine() {

		AddRemarkModel model = buildAddRemarkModel();

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setRemark("First Remark");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), isNull())).thenReturn(new Response());

		updateActivityService.addRemark(model);

		String remark = project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0)
				.getActivities().get(0).getRemark();

		assertTrue(remark.startsWith("First Remark"));

		assertTrue(remark.contains(System.lineSeparator()));

		assertTrue(remark.endsWith("Activity completed successfully"));

		verify(projectRepository).save(project);
	}

	@Test
	void addRemark_BlankExistingRemark_ShouldReplaceRemark() {

		AddRemarkModel model = buildAddRemarkModel();

		Project project = buildProject();

		Response response = new Response();

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setRemark("   ");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), isNull())).thenReturn(response);

		Response result = updateActivityService.addRemark(model);

		assertNotNull(result);

		assertEquals("Activity completed successfully", project.getPhases().get(0).getMilestones().get(0).getTasks()
				.get(0).getSubTasks().get(0).getActivities().get(0).getRemark());

		verify(projectRepository).save(project);

		verify(responseBuilder).createResponse(any(), any(), eq("Remark added successfully"), isNull());
	}

	@Test
	void updateActivityRequest_ShouldPopulateActivityUpdateRequestCorrectly() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = buildProject();

		Response response = new Response();

		ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.empty());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		updateActivityService.updateActivityRequest(request);

		verify(requestRepository).save(captor.capture());

		ActivityUpdateRequest saved = captor.getValue();

		assertAll(

				() -> assertEquals("P001", saved.getProjectId()),

				() -> assertEquals("Phase1", saved.getPhaseName()),

				() -> assertEquals("Milestone1", saved.getMilestoneName()),

				() -> assertEquals("Task1", saved.getTaskName()),

				() -> assertEquals("SubTask1", saved.getSubTaskName()),

				() -> assertEquals("Activity1", saved.getActivityName()),

				() -> assertEquals("Updating progress", saved.getChangeReason()),

				() -> assertEquals("PENDING", saved.getStatus()),

				() -> assertEquals("MANUAL", saved.getRequestSource()),

				() -> assertNotNull(saved.getRequestedAt()));
	}

	@Test
	void updateActivityRequest_ShouldCopyOldActivityCorrectly() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = buildProject();

		ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.empty());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		updateActivityService.updateActivityRequest(request);

		verify(requestRepository).save(captor.capture());

		Activity oldActivity = captor.getValue().getOldActivity();

		assertAll(

				() -> assertEquals("Activity1", oldActivity.getActivityName()),

				() -> assertEquals(2.0, oldActivity.getEstimatedPeriodWeek()),

				() -> assertEquals(50, oldActivity.getProgress()));
	}

	@Test
	void updateActivityRequest_ShouldPopulateRequesterInformation() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = buildProject();

		ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.empty());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		updateActivityService.updateActivityRequest(request);

		verify(requestRepository).save(captor.capture());

		ActivityUpdateRequest saved = captor.getValue();

		assertEquals("admin", saved.getRequestedBy());

		assertNotNull(saved.getRequestedAt());

		assertEquals("MANUAL", saved.getRequestSource());

		assertEquals("PENDING", saved.getStatus());
	}

	@Test
	void updateActivityRequest_ShouldPopulateCalculatedFields() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		request.setProgress(100);

		request.setActualStartDate(LocalDate.of(2025, 1, 1));

		request.setActualEndDate(LocalDate.of(2025, 1, 15));

		Project project = buildProject();

		ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.empty());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		updateActivityService.updateActivityRequest(request);

		verify(requestRepository).save(captor.capture());

		Activity newActivity = captor.getValue().getNewActivity();

		assertNotNull(newActivity.getActualPeriodWeek());

		assertNotNull(newActivity.getExecutionStatus());

		assertNotNull(newActivity.getScheduleHealth());
	}

}