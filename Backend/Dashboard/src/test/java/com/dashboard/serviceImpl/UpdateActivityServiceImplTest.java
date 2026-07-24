package com.dashboard.serviceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

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
import com.novillex.progresstracker.model.ActivityUpdateRequestModel;
import com.novillex.progresstracker.model.AddRemarkModel;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.serviceImpl.UpdateActivityServiceImpl;
import com.novillex.progresstracker.util.UserContextUtil;
import com.novillex.progresstracker.util.WriteUtil;

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

		// Project
		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// IDs (Required by validateUpdateRequest)
		request.setPhaseId("PH001");
		request.setMilestoneId("M001");
		request.setTaskId("T001");
		request.setSubTaskId("ST001");
		request.setActivityId("ACT001");

		// Names
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		// Activity Details
		request.setOwner("Sachin");
		request.setEstimatedPeriodWeek(5.0);

		request.setPlannedStartDate(LocalDate.of(2025, 1, 1));
		request.setPlannedEndDate(LocalDate.of(2025, 1, 15));

		request.setActualStartDate(LocalDate.of(2025, 1, 2));
		request.setActualEndDate(LocalDate.of(2025, 1, 16));

		request.setProgress(80);

		request.setChangeReason("Updated Remark");
		request.setChangeReason("Updating progress");

		return request;
	}

	private Project buildProject() {

		Activity activity = new Activity();

		activity.setActivityId("ACT001");
		activity.setActivityName("Activity1");
		activity.setEstimatedPeriodWeek(2.0);
		activity.setProgress(50);

		Subtask subtask = new Subtask();

		subtask.setSubTaskId("ST001");
		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(new ArrayList<>(List.of(activity)));

		Task task = new Task();

		task.setTaskId("T001");
		task.setTaskName("Task1");
		task.setSubTasks(new ArrayList<>(List.of(subtask)));

		Milestone milestone = new Milestone();

		milestone.setMilestoneId("M001");
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(new ArrayList<>(List.of(task)));

		Phase phase = new Phase();

		phase.setPhaseId("PH001");
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

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		request.setProgress(80);

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setProgress(50);

		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		when(requestRepository.findByActivityIdAndStatus("ACT001", "PENDING")).thenReturn(Optional.empty());

		when(requestRepository.save(any(ActivityUpdateRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(),
				nullable(String.class), anyString(), isNull());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			Response result = updateActivityService.updateActivityRequest(request);

			assertNotNull(result);
			assertSame(response, result);
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository).findByActivityIdAndStatus("ACT001", "PENDING");

		verify(requestRepository).save(any(ActivityUpdateRequest.class));

		verify(notificationService).createNotification(eq("Activity Update Requested"), contains("Activity1"),
				eq("ACTIVITY_UPDATE"), any(), contains("/authorization?type=activity-update&requestId="), isNull());

		verify(auditService).saveAuditLog(any(), any(), anyString(), eq("Demo Project"), any(), any(), eq("admin"));
	}

	@Test
	void updateActivityRequest_ProjectNotFound() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.empty());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
					() -> updateActivityService.updateActivityRequest(request));

			assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.getErrorCode());
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository, never()).findByActivityIdAndStatus(anyString(), anyString());

		verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));

		verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_ActivityNotFound() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
					() -> updateActivityService.updateActivityRequest(request));

			assertEquals(ErrorCode.ACTIVITY_NOT_FOUND, exception.getErrorCode());
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository, never()).findByActivityIdAndStatus(anyString(), anyString());

		verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));

		verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_NoChangesFound() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		// Same values as existing activity
		request.setEstimatedPeriodWeek(2.0);
		request.setProgress(50);

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);

		Activity activity = project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0)
				.getActivities().get(0);

		activity.setActivityId("ACT001");
		activity.setActivityName(request.getActivityName());
		activity.setOwner(request.getOwner());

		activity.setEstimatedPeriodWeek(request.getEstimatedPeriodWeek());

		activity.setPlannedStartDate(request.getPlannedStartDate());
		activity.setPlannedEndDate(request.getPlannedEndDate());

		activity.setActualStartDate(request.getActualStartDate());
		activity.setActualEndDate(request.getActualEndDate());

		Double actualPeriodWeek = WriteUtil.calculateActualPeriodWeek(request.getActualStartDate(),
				request.getActualEndDate());

		activity.setActualPeriodWeek(actualPeriodWeek);

		activity.setProgress(request.getProgress());

		activity.setExecutionStatus(WriteUtil.calculateExecutionStatus(request.getProgress()));

		activity.setScheduleHealth(WriteUtil.calculateScheduleHealth(request.getProgress(),
				request.getPlannedStartDate(), request.getPlannedEndDate(), request.getActualStartDate(),
				request.getActualEndDate(), actualPeriodWeek));

		// Must match request exactly
		activity.setRemark(request.getChangeReason());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");

			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			ValidationException exception = assertThrows(ValidationException.class,
					() -> updateActivityService.updateActivityRequest(request));

			assertEquals(ErrorCode.NO_CHANGES_FOUND, exception.getErrorCode());
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository, never()).save(any(ActivityUpdateRequest.class));

		verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_SaveRequestThrowsException() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		request.setProgress(80);

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setProgress(50);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		when(requestRepository.findByActivityIdAndStatus("ACT001", "PENDING")).thenReturn(Optional.empty());

		doThrow(new RuntimeException("Database Error")).when(requestRepository).save(any(ActivityUpdateRequest.class));

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");

			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			RuntimeException exception = assertThrows(RuntimeException.class,
					() -> updateActivityService.updateActivityRequest(request));

			assertEquals("Database Error", exception.getMessage());
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository).findByActivityIdAndStatus("ACT001", "PENDING");

		verify(requestRepository).save(any(ActivityUpdateRequest.class));

		verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_NotificationThrowsException() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();
		request.setProgress(80);

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setProgress(50);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		when(requestRepository.findByActivityIdAndStatus("ACT001", "PENDING")).thenReturn(Optional.empty());

		when(requestRepository.save(any(ActivityUpdateRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		doThrow(new RuntimeException("Notification Error")).when(notificationService).createNotification(anyString(),
				anyString(), anyString(), nullable(String.class), anyString(), isNull());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			RuntimeException ex = assertThrows(RuntimeException.class,
					() -> updateActivityService.updateActivityRequest(request));

			assertEquals("Notification Error", ex.getMessage());
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository).findByActivityIdAndStatus("ACT001", "PENDING");

		verify(requestRepository).save(any(ActivityUpdateRequest.class));

		verify(notificationService).createNotification(eq("Activity Update Requested"), anyString(),
				eq("ACTIVITY_UPDATE"), any(), contains("/authorization?type=activity-update&requestId="), isNull());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void updateActivityRequest_AuditServiceThrowsException() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();
		request.setProgress(80);

		Project project = buildProject();
		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);
		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setProgress(50);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		when(requestRepository.findByActivityIdAndStatus("ACT001", "PENDING")).thenReturn(Optional.empty());

		when(requestRepository.save(any(ActivityUpdateRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(),
				nullable(String.class), anyString(), isNull());

		doThrow(new RuntimeException("Audit Error")).when(auditService).saveAuditLog(any(), any(), any(), any(), any(),
				any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			RuntimeException ex = assertThrows(RuntimeException.class,
					() -> updateActivityService.updateActivityRequest(request));

			assertEquals("Audit Error", ex.getMessage());
		}

		verify(requestRepository).save(any(ActivityUpdateRequest.class));

		verify(notificationService).createNotification(eq("Activity Update Requested"), contains("Activity1"),
				eq("ACTIVITY_UPDATE"), any(), contains("/authorization?type=activity-update&requestId="), isNull());

		verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	private AddRemarkModel buildAddRemarkModel() {

		AddRemarkModel model = new AddRemarkModel();

		model.setProjectId("P001");
		model.setPhaseId("PH001");
		model.setMilestoneId("M001");
		model.setTaskId("T001");
		model.setSubTaskId("ST001");
		model.setActivityId("ACT001");

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

		// Make the activity ID different so it cannot be found
		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setActivityId("ACT002");

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

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();
		request.setProgress(80);

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setProgress(50);

		Response response = new Response();

		ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		when(requestRepository.findByActivityIdAndStatus("ACT001", "PENDING")).thenReturn(Optional.empty());

		when(requestRepository.save(any(ActivityUpdateRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			updateActivityService.updateActivityRequest(request);
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository).findByActivityIdAndStatus("ACT001", "PENDING");

		verify(requestRepository).save(captor.capture());

		ActivityUpdateRequest saved = captor.getValue();

		assertNotNull(saved);

		assertAll(() -> assertEquals("P001", saved.getProjectId()),
				() -> assertEquals("Demo Project", saved.getProjectName()),
				() -> assertEquals("ACT001", saved.getActivityId()),

				() -> assertEquals("Phase1", saved.getOldPhaseName()),
				() -> assertEquals("Milestone1", saved.getOldMilestoneName()),
				() -> assertEquals("Task1", saved.getOldTaskName()),
				() -> assertEquals("SubTask1", saved.getOldSubTaskName()),
				() -> assertEquals("Activity1", saved.getOldActivityName()),

				() -> assertEquals("Phase1", saved.getNewPhaseName()),
				() -> assertEquals("Milestone1", saved.getNewMilestoneName()),
				() -> assertEquals("Task1", saved.getNewTaskName()),
				() -> assertEquals("SubTask1", saved.getNewSubTaskName()),
				() -> assertEquals("Activity1", saved.getNewActivityName()),

				() -> assertEquals("Updating progress", saved.getChangeReason()),

				() -> assertEquals("admin", saved.getRequestedBy()),
				() -> assertEquals("USER001", saved.getRequestedByUserId()),
				() -> assertEquals("ADMIN", saved.getRequestedByRole()),

				() -> assertEquals("PENDING", saved.getStatus()),
				() -> assertEquals("MANUAL", saved.getRequestSource()), () -> assertNotNull(saved.getRequestedAt()),

				() -> assertNotNull(saved.getOldActivity()), () -> assertNotNull(saved.getNewActivity()));
	}

	@Test
	void updateActivityRequest_ShouldCopyOldActivityCorrectly() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();
		request.setProgress(80);

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);

		Activity existingActivity = project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks()
				.get(0).getActivities().get(0);

		existingActivity.setActivityId("ACT001");
		existingActivity.setActivityName("Activity1");
		existingActivity.setEstimatedPeriodWeek(2.0);
		existingActivity.setProgress(50);

		ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		when(requestRepository.findByActivityIdAndStatus("ACT001", "PENDING")).thenReturn(Optional.empty());

		when(requestRepository.save(any(ActivityUpdateRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		doNothing().when(notificationService).createNotification(anyString(), anyString(), anyString(),
				nullable(String.class), anyString(), isNull());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			updateActivityService.updateActivityRequest(request);
		}

		verify(requestRepository).save(captor.capture());

		Activity oldActivity = captor.getValue().getOldActivity();

		assertNotNull(oldActivity);

		assertAll(() -> assertEquals("ACT001", oldActivity.getActivityId()),
				() -> assertEquals("Activity1", oldActivity.getActivityName()),
				() -> assertEquals(2.0, oldActivity.getEstimatedPeriodWeek()),
				() -> assertEquals(50, oldActivity.getProgress()));
	}

	@Test
	void updateActivityRequest_ShouldPopulateRequesterInformation() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();
		request.setProgress(80);

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);

		project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks().get(0).getActivities().get(0)
				.setProgress(50);

		ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		when(requestRepository.findByActivityIdAndStatus("ACT001", "PENDING")).thenReturn(Optional.empty());

		when(requestRepository.save(any(ActivityUpdateRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			updateActivityService.updateActivityRequest(request);
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository).findByActivityIdAndStatus("ACT001", "PENDING");

		verify(requestRepository).save(captor.capture());

		ActivityUpdateRequest saved = captor.getValue();

		assertNotNull(saved);

		assertEquals("admin", saved.getRequestedBy());
		assertEquals("USER001", saved.getRequestedByUserId());
		assertEquals("ADMIN", saved.getRequestedByRole());

		assertNotNull(saved.getRequestedAt());

		// Updated expectation
		assertEquals("MANUAL", saved.getRequestSource());

		assertEquals("PENDING", saved.getStatus());
	}

	@Test
	void updateActivityRequest_ShouldPopulateCalculatedFields() {

		ActivityUpdateRequestModel request = buildActivityUpdateRequestModel();

		request.setProgress(100);
		request.setActualStartDate(LocalDate.of(2025, 1, 1));
		request.setActualEndDate(LocalDate.of(2025, 1, 15));

		Project project = buildProject();

		project.getPhases().get(0).getMilestones().get(0).setWeightage(100.0);

		Activity existingActivity = project.getPhases().get(0).getMilestones().get(0).getTasks().get(0).getSubTasks()
				.get(0).getActivities().get(0);

		existingActivity.setActivityId("ACT001");
		existingActivity.setProgress(50);
		existingActivity.setActualStartDate(LocalDate.of(2025, 1, 2));
		existingActivity.setActualEndDate(LocalDate.of(2025, 1, 10));

		ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001"))
				.thenReturn(Optional.of(project));

		when(requestRepository.findByActivityIdAndStatus("ACT001", "PENDING")).thenReturn(Optional.empty());

		when(requestRepository.save(any(ActivityUpdateRequest.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		doNothing().when(notificationService).createNotification(any(), any(), any(), any(), any(), any());

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");
			mocked.when(UserContextUtil::getCurrentUserId).thenReturn("USER001");
			mocked.when(UserContextUtil::getCurrentUserRole).thenReturn("ADMIN");

			updateActivityService.updateActivityRequest(request);
		}

		verify(projectRepository).findByPhasesMilestonesTasksSubTasksActivitiesActivityId("ACT001");

		verify(requestRepository).findByActivityIdAndStatus("ACT001", "PENDING");

		verify(requestRepository).save(captor.capture());

		Activity newActivity = captor.getValue().getNewActivity();

		assertNotNull(newActivity);

		assertAll(() -> assertNotNull(newActivity.getActualPeriodWeek()),
				() -> assertNotNull(newActivity.getExecutionStatus()),
				() -> assertNotNull(newActivity.getScheduleHealth()),
				() -> assertEquals(100, newActivity.getProgress()),
				() -> assertEquals(LocalDate.of(2025, 1, 1), newActivity.getActualStartDate()),
				() -> assertEquals(LocalDate.of(2025, 1, 15), newActivity.getActualEndDate()));
	}
}