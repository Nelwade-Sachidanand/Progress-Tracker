package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.serviceImpl.CreateStructureServiceImpl;

@ExtendWith(MockitoExtension.class)

public class CreateStructureServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ApplicationContext context;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private CreateStructureServiceImpl createStructureService;

	private void mockLoggedInUser() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, new ArrayList<>()));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createStructure_ActivityAlreadyExists() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Activity activity = new Activity();
		activity.setActivityName("Activity1");

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

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		assertThrows(ValidationException.class, () -> createStructureService.createStructure(request));

		verify(projectRepository, never()).save(any());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_ProjectNotFound() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> createStructureService.createStructure(request));

		assertEquals("Project not found", exception.getMessage());

		verify(projectRepository).findById("P001");

		verify(projectRepository, never()).save(any());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_SaveProjectFailure() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		doThrow(new RuntimeException("DB Error")).when(projectRepository).save(any(Project.class));

		DatabaseException exception = assertThrows(DatabaseException.class,
				() -> createStructureService.createStructure(request));

		assertNotNull(exception);

		verify(projectRepository).findById("P001");

		verify(projectRepository).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_CreateNewPhaseSuccessfully() {
		mockLoggedInUser();
		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), any()))
				.thenReturn(mock(com.novillex.progresstracker.common.Response.class));

		assertDoesNotThrow(() -> createStructureService.createStructure(request));

		verify(projectRepository).save(project);

		assertEquals(1, project.getPhases().size());

		assertEquals("Phase1", project.getPhases().get(0).getPhaseName());

		verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_CreateNewMilestoneSuccessfully() {

		mockLoggedInUser();

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>());

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), any()))
				.thenReturn(mock(com.novillex.progresstracker.common.Response.class));

		assertDoesNotThrow(() -> createStructureService.createStructure(request));

		verify(projectRepository).save(project);

		assertEquals(1, phase.getMilestones().size());

		assertEquals("Milestone1", phase.getMilestones().get(0).getMilestoneName());

		verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_CreateNewTaskSuccessfully() {

		mockLoggedInUser();

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(new ArrayList<>());

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>(List.of(milestone)));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), any()))
				.thenReturn(mock(com.novillex.progresstracker.common.Response.class));

		assertDoesNotThrow(() -> createStructureService.createStructure(request));

		verify(projectRepository).save(project);

		assertEquals(1, milestone.getTasks().size());

		assertEquals("Task1", milestone.getTasks().get(0).getTaskName());

		verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_CreateNewSubTaskSuccessfully() {

		mockLoggedInUser();

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Task task = new Task();
		task.setTaskName("Task1");
		task.setSubTasks(new ArrayList<>());

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

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), any()))
				.thenReturn(mock(com.novillex.progresstracker.common.Response.class));

		assertDoesNotThrow(() -> createStructureService.createStructure(request));

		verify(projectRepository).save(project);

		assertEquals(1, task.getSubTasks().size());

		assertEquals("SubTask1", task.getSubTasks().get(0).getSubTaskName());

		verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_CreateNewActivitySuccessfully() {

		mockLoggedInUser();

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");
		request.setProgress(50);

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(new ArrayList<>());

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

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), any()))
				.thenReturn(mock(com.novillex.progresstracker.common.Response.class));

		assertDoesNotThrow(() -> createStructureService.createStructure(request));

		verify(projectRepository).save(project);

		assertEquals(1, subtask.getActivities().size());

		assertEquals("Activity1", subtask.getActivities().get(0).getActivityName());

		verify(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_ProjectHasNullPhases() {

		mockLoggedInUser();

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(null);

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), any()))
				.thenReturn(mock(com.novillex.progresstracker.common.Response.class));

		assertDoesNotThrow(() -> createStructureService.createStructure(request));

		verify(projectRepository).save(project);

		assertNotNull(project.getPhases());

		assertEquals(1, project.getPhases().size());
	}

	@Test
	void createStructure_ShouldReturnResponseFromResponseBuilder() {

		mockLoggedInUser();

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");
		request.setProgress(50);

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(new ArrayList<>());

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

		Response expectedResponse = mock(Response.class);

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), eq(project))).thenReturn(expectedResponse);

		Response actual = createStructureService.createStructure(request);

		assertSame(expectedResponse, actual);

		verify(responseBuilder).createResponse(any(), any(), eq("Activity created successfully"), eq(project));
	}

}
