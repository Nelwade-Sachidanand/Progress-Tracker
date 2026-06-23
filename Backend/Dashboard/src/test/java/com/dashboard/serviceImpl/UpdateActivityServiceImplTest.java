package com.dashboard.serviceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.novillex.progresstracker.common.ErrorCode;
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
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.serviceImpl.UpdateActivityServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

	@Test
	void shouldThrowProjectNotFoundException() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Progress Tracker");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		request.setEstimatedPeriodWeek(2.0);

		request.setProgress(50);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
				() -> updateActivityService.updateActivity(request));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, ex.getErrorCode());
	}

	@Test
	void shouldThrowActivityNotFoundException() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testUser", null));

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Progress Tracker");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Project project = new Project();
		project.setProjectName("Progress Tracker");

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone1");

		Task task = new Task();
		task.setTaskName("Task1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask1");

		subtask.setActivities(new ArrayList<>());

		task.setSubTasks(List.of(subtask));
		milestone.setTasks(List.of(task));
		phase.setMilestones(List.of(milestone));
		project.setPhases(List.of(phase));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
				() -> updateActivityService.updateActivity(request));

		assertEquals(ErrorCode.ACTIVITY_NOT_FOUND, ex.getErrorCode());
	}

	@Test
	void shouldThrowNoChangesFoundException() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Progress Tracker");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		request.setEstimatedPeriodWeek(2.0);
		request.setProgress(50);

		Activity activity = new Activity();

		activity.setActivityName("Activity1");
		activity.setEstimatedPeriodWeek(2.0);
		activity.setProgress(50);

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
		project.setPhases(List.of(phase));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ValidationException ex = assertThrows(ValidationException.class,
				() -> updateActivityService.updateActivity(request));

		assertEquals(ErrorCode.NO_CHANGES_FOUND, ex.getErrorCode());
	}

	@Test
	void shouldUpdateActivityRequestSuccessfully() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testUser", null));

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Progress Tracker");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		request.setEstimatedPeriodWeek(5.0);
		request.setProgress(80);

		Activity activity = new Activity();

		activity.setActivityName("Activity1");
		activity.setEstimatedPeriodWeek(2.0);
		activity.setProgress(50);

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
		project.setProjectName("Progress Tracker");
		project.setPhases(List.of(phase));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), any()))
				.thenReturn(Optional.empty());

		updateActivityService.updateActivity(request);

		verify(requestRepository, times(1)).save(any());

		verify(notificationService, times(1)).createNotification(anyString(), anyString(), anyString(), any(),
				anyString());

		verify(auditService, times(1)).saveAuditLog(any(), any(), anyString(), anyString(), any(), any(), anyString());
	}

	@Test
	void shouldThrowRequestAlreadyPendingException() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Progress Tracker");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Project project = new Project();

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone1");

		Task task = new Task();
		task.setTaskName("Task1");

		Activity activity = new Activity();

		activity.setActivityName("Activity1");
		activity.setProgress(20);

		Subtask subtask = new Subtask();

		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(List.of(activity));

		task.setSubTasks(List.of(subtask));
		milestone.setTasks(List.of(task));
		phase.setMilestones(List.of(milestone));
		project.setPhases(List.of(phase));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(requestRepository
				.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
						any(), any(), any(), any(), any(), eq("PENDING")))
				.thenReturn(Optional.of(new ActivityUpdateRequest()));

		ValidationException ex = assertThrows(ValidationException.class,
				() -> updateActivityService.updateActivity(request));

		assertEquals(ErrorCode.REQUEST_ALREADY_PENDING, ex.getErrorCode());
	}

	@Test
	void shouldHandleNullActivitiesList() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Progress Tracker");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(null);

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
		project.setPhases(List.of(phase));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		assertThrows(ResourceNotFoundException.class, () -> updateActivityService.updateActivity(request));

	}

}