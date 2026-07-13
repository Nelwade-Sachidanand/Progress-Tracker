package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
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
import com.novillex.progresstracker.util.UserContextUtil;

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

	private ActivityModel buildNewPhaseRequest() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		return request;
	}

	@Test
	void createStructure_ActivityAlreadyExists() {

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		request.setTaskId("T001");
		request.setTaskName("Task1");

		request.setSubTaskId("ST001");
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		Activity activity = new Activity();
		activity.setActivityId("ACT001");
		activity.setActivityName("Activity1");

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

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		assertThrows(ValidationException.class, () -> createStructureService.createStructure(request));

		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_ProjectNotFound() {

		ActivityModel request = buildNewPhaseRequest();

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
		when(projectRepository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");
		verify(projectRepository, never()).save(any());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_SaveProjectFailure() {

		ActivityModel request = buildNewPhaseRequest();

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

		ActivityModel request = buildNewPhaseRequest();

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
		when(projectRepository.save(any(Project.class))).thenReturn(project);
		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		assertDoesNotThrow(() -> createStructureService.createStructure(request));

		verify(projectRepository).save(project);

		assertEquals(1, project.getPhases().size());
		assertEquals("Phase1", project.getPhases().get(0).getPhaseName());

		verify(auditService).saveAuditLog(eq(AuditAction.CREATE_PHASE), eq(AuditEntity.PHASE), eq("Phase1"),
				eq("Demo Project"), isNull(), any(Phase.class), eq("admin"));
	}

	@Test
	void createStructure_CreateNewMilestoneSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// New Milestone
		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		// New Task (Required for validation)
		request.setNewTask(true);
		request.setTaskName("Task1");

		// New SubTask (Required for validation)
		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		// Activity (Required for validation)
		request.setActivityName("Activity1");

		Phase phase = new Phase();
		phase.setPhaseId("PH001");
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>());

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(projectRepository.save(any(Project.class))).thenReturn(project);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			assertDoesNotThrow(() -> createStructureService.createStructure(request));
		}

		verify(projectRepository).save(project);

		assertEquals(1, phase.getMilestones().size());
		assertEquals("Milestone1", phase.getMilestones().get(0).getMilestoneName());

		verify(auditService).saveAuditLog(eq(AuditAction.CREATE_MILESTONE), eq(AuditEntity.MILESTONE), eq("Milestone1"),
				eq("Demo Project"), isNull(), any(Milestone.class), eq("admin"));
	}

	@Test
	void createStructure_CreateNewTaskSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// Existing Milestone
		request.setNewMilestone(false);
		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		// New Task
		request.setNewTask(true);
		request.setTaskName("Task1");

		// New SubTask (Required for validation)
		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		// Activity (Required for validation)
		request.setActivityName("Activity1");

		Milestone milestone = new Milestone();
		milestone.setMilestoneId("M001");
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(new ArrayList<>());

		Phase phase = new Phase();
		phase.setPhaseId("PH001");
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>(List.of(milestone)));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(projectRepository.save(any(Project.class))).thenReturn(project);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			assertDoesNotThrow(() -> createStructureService.createStructure(request));
		}

		verify(projectRepository).save(project);

		assertEquals(1, milestone.getTasks().size());
		assertEquals("Task1", milestone.getTasks().get(0).getTaskName());

		verify(auditService).saveAuditLog(eq(AuditAction.CREATE_TASK), eq(AuditEntity.TASK), eq("Task1"),
				eq("Demo Project"), isNull(), any(Task.class), eq("admin"));
	}

	@Test
	void createStructure_CreateNewSubTaskSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// Existing Milestone
		request.setNewMilestone(false);
		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		// Existing Task
		request.setNewTask(false);
		request.setTaskId("T001");
		request.setTaskName("Task1");

		// New SubTask
		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		// Required for validation
		request.setActivityName("Activity1");

		Task task = new Task();
		task.setTaskId("T001");
		task.setTaskName("Task1");
		task.setSubTasks(new ArrayList<>());

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

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(projectRepository.save(any(Project.class))).thenReturn(project);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			assertDoesNotThrow(() -> createStructureService.createStructure(request));
		}

		verify(projectRepository).save(project);

		assertEquals(1, task.getSubTasks().size());
		assertEquals("SubTask1", task.getSubTasks().get(0).getSubTaskName());

		verify(auditService).saveAuditLog(eq(AuditAction.CREATE_SUBTASK), eq(AuditEntity.SUBTASK), eq("SubTask1"),
				eq("Demo Project"), isNull(), any(Subtask.class), eq("admin"));
	}

	@Test
	void createStructure_CreateNewActivitySuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		request.setTaskId("T001");
		request.setTaskName("Task1");

		request.setSubTaskId("ST001");
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");
		request.setProgress(50);

		Subtask subtask = new Subtask();
		subtask.setSubTaskId("ST001");
		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(new ArrayList<>());

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

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(projectRepository.save(any(Project.class))).thenReturn(project);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			assertDoesNotThrow(() -> createStructureService.createStructure(request));
		}

		verify(projectRepository).save(project);

		assertEquals(1, subtask.getActivities().size());

		assertEquals("Activity1", subtask.getActivities().get(0).getActivityName());

		verify(auditService).saveAuditLog(eq(AuditAction.CREATE_ACTIVITY), eq(AuditEntity.ACTIVITY), eq("Activity1"),
				eq("Demo Project"), isNull(), any(Activity.class), eq("admin"));
	}

	@Test
	void createStructure_ShouldReturnResponseFromResponseBuilder() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response expectedResponse = mock(Response.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		request.setTaskId("T001");
		request.setTaskName("Task1");

		request.setSubTaskId("ST001");
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");
		request.setProgress(50);

		Subtask subtask = new Subtask();
		subtask.setSubTaskId("ST001");
		subtask.setSubTaskName("SubTask1");
		subtask.setActivities(new ArrayList<>());

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

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
		when(projectRepository.save(any(Project.class))).thenReturn(project);

		when(responseBuilder.createResponse(any(), any(), eq("Activity created successfully"), eq(project)))
				.thenReturn(expectedResponse);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("admin");

			Response actual = createStructureService.createStructure(request);

			assertSame(expectedResponse, actual);
		}

		verify(projectRepository).save(project);

		verify(responseBuilder).createResponse(any(), any(), eq("Activity created successfully"), eq(project));

		verify(auditService).saveAuditLog(eq(AuditAction.CREATE_ACTIVITY), eq(AuditEntity.ACTIVITY), eq("Activity1"),
				eq("Demo Project"), isNull(), any(Activity.class), eq("admin"));
	}

	@Test
	void createStructure_PhaseAlreadyExists() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		Phase existingPhase = new Phase();
		existingPhase.setPhaseId("PH001");
		existingPhase.setPhaseName("Phase1");

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(existingPhase)));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.PHASE_ALREADY_EXISTS, exception.getErrorCode());

		verify(projectRepository).findById("P001");
		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_MilestoneAlreadyExists() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// New Milestone (Duplicate)
		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		// Required for validation
		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		Milestone existingMilestone = new Milestone();
		existingMilestone.setMilestoneId("M001");
		existingMilestone.setMilestoneName("Milestone1");

		Phase phase = new Phase();
		phase.setPhaseId("PH001");
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>(List.of(existingMilestone)));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.MILESTONE_ALREADY_EXISTS, exception.getErrorCode());

		verify(projectRepository).findById("P001");
		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_TaskAlreadyExists() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// Existing Milestone
		request.setNewMilestone(false);
		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		// New Task (Duplicate)
		request.setNewTask(true);
		request.setTaskName("Task1");

		// Required for validation
		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		Task existingTask = new Task();
		existingTask.setTaskId("T001");
		existingTask.setTaskName("Task1");

		Milestone milestone = new Milestone();
		milestone.setMilestoneId("M001");
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(new ArrayList<>(List.of(existingTask)));

		Phase phase = new Phase();
		phase.setPhaseId("PH001");
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>(List.of(milestone)));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.TASK_ALREADY_EXISTS, exception.getErrorCode());

		verify(projectRepository).findById("P001");
		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_SubTaskAlreadyExists() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// Existing Milestone
		request.setNewMilestone(false);
		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		// Existing Task
		request.setNewTask(false);
		request.setTaskId("T001");
		request.setTaskName("Task1");

		// New SubTask (Duplicate)
		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		// Required for validation
		request.setActivityName("Activity1");

		Subtask existingSubTask = new Subtask();
		existingSubTask.setSubTaskId("ST001");
		existingSubTask.setSubTaskName("SubTask1");

		Task task = new Task();
		task.setTaskId("T001");
		task.setTaskName("Task1");
		task.setSubTasks(new ArrayList<>(List.of(existingSubTask)));

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

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.SUBTASK_ALREADY_EXISTS, exception.getErrorCode());

		verify(projectRepository).findById("P001");
		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_PhaseNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase (Invalid Id)
		request.setNewPhase(false);
		request.setPhaseId("INVALID");
		request.setPhaseName("Phase1");

		// Required for validation
		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.PHASE_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");

		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_MilestoneNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// Existing Milestone (Invalid Id)
		request.setNewMilestone(false);
		request.setMilestoneId("INVALID");
		request.setMilestoneName("Milestone1");

		// Required for validation
		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		Phase phase = new Phase();
		phase.setPhaseId("PH001");
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>());

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.MILESTONE_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");

		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_TaskNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// Existing Milestone
		request.setNewMilestone(false);
		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		// Existing Task (Invalid Id)
		request.setNewTask(false);
		request.setTaskId("INVALID");
		request.setTaskName("Task1");

		// Required for validation
		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		Milestone milestone = new Milestone();
		milestone.setMilestoneId("M001");
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(new ArrayList<>());

		Phase phase = new Phase();
		phase.setPhaseId("PH001");
		phase.setPhaseName("Phase1");
		phase.setMilestones(new ArrayList<>(List.of(milestone)));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>(List.of(phase)));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.TASK_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");
		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_SubTaskNotFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		request.setPhaseId("PH001");
		request.setPhaseName("Phase1");

		// Existing Milestone
		request.setNewMilestone(false);
		request.setMilestoneId("M001");
		request.setMilestoneName("Milestone1");

		// Existing Task
		request.setNewTask(false);
		request.setTaskId("T001");
		request.setTaskName("Task1");

		// Existing SubTask (Invalid Id)
		request.setNewSubTask(false);
		request.setSubTaskId("INVALID");
		request.setSubTaskName("SubTask1");

		// Required for validation
		request.setActivityName("Activity1");

		Task task = new Task();
		task.setTaskId("T001");
		task.setTaskName("Task1");
		task.setSubTasks(new ArrayList<>());

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

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.SUBTASK_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");
		verify(projectRepository, never()).save(any(Project.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_InvalidProgress() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		request.setProgress(120); // Invalid (>100)

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.INVALID_PROGRESS, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_InvalidPlannedDates() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		request.setPlannedStartDate(LocalDate.of(2025, 5, 10));
		request.setPlannedEndDate(LocalDate.of(2025, 5, 1)); // Invalid

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.INVALID_PLANNED_DATES, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_InvalidActualDates() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		request.setActualStartDate(LocalDate.of(2025, 5, 10));
		request.setActualEndDate(LocalDate.of(2025, 5, 1)); // Invalid

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.INVALID_ACTUAL_DATES, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_ActualStartWithoutPlannedStart() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		request.setActualStartDate(LocalDate.of(2025, 5, 10));
		// Planned Start Date not set

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.PLANNED_DATE_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_ActualEndWithoutActualStart() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		request.setPlannedStartDate(LocalDate.of(2025, 5, 1));
		request.setActualEndDate(LocalDate.of(2025, 5, 10));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.ACTUAL_START_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_InvalidEstimatedPeriodWeek() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		request.setEstimatedPeriodWeek(0.0);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.ESTIMATED_PERIOD_INVALID, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_ActivityNameRequired() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		// Activity Name not set

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.ACTIVITY_NAME_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_ProjectIdRequired() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		// Project Id not set
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.PROJECT_ID_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_PhaseNameRequired() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		// Phase name not set

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.PHASE_NAME_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_MilestoneNameRequired() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		// Milestone name not set

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.MILESTONE_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_TaskNameRequired() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		// Task name not set

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.TASK_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_SubTaskNameRequired() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(true);
		request.setPhaseName("Phase1");

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		// SubTask name not set

		request.setActivityName("Activity1");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.SUBTASK_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_PhaseIdRequired() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		// Existing Phase
		request.setNewPhase(false);
		// Phase Id not set

		request.setNewMilestone(true);
		request.setMilestoneName("Milestone1");

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.PHASE_ID_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void createStructure_MilestoneIdRequired() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ActivityModel request = new ActivityModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		request.setNewPhase(false);
		request.setPhaseId("PH001");

		// Existing Milestone
		request.setNewMilestone(false);
		// Milestone Id not set

		request.setNewTask(true);
		request.setTaskName("Task1");

		request.setNewSubTask(true);
		request.setSubTaskName("SubTask1");

		request.setActivityName("Activity1");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		ValidationException exception = assertThrows(ValidationException.class,
				() -> createStructureService.createStructure(request));

		assertEquals(ErrorCode.MILESTONE_ID_REQUIRED, exception.getErrorCode());

		verifyNoInteractions(projectRepository);

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}
}
