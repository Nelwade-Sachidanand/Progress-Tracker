package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.mapper.ActivityMapper;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.GenerateReportModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.serviceImpl.ReportServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ActivityMapper mapper;

	@InjectMocks
	private ReportServiceImpl reportService;

	@Test
	void generateReport_WhenProjectIdIsBlank_ShouldThrowValidationException() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("");
		request.setProjectName("Demo Project");

		ValidationException exception = assertThrows(ValidationException.class,
				() -> reportService.generateReport(request));

		assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

		verifyNoInteractions(projectRepository);
		verifyNoInteractions(mapper);
	}

	@Test
	void generateReport_WhenProjectNameIsBlank_ShouldThrowValidationException() {

	    GenerateReportModel request = new GenerateReportModel();

	    request.setProjectId("P001");
	    request.setProjectName("");

	    ValidationException exception = assertThrows(
	            ValidationException.class,
	            () -> reportService.generateReport(request));

	    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
	    assertEquals("Project Name is required", exception.getMessage());

	    verifyNoInteractions(projectRepository);
	    verifyNoInteractions(mapper);
	}

	@Test
	void generateReport_WhenMilestoneSelectedWithoutPhase_ShouldThrowValidationException() {

	    // Arrange
	    GenerateReportModel request = new GenerateReportModel();

	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");

	    List<String> milestoneIds = new ArrayList<>();
	    milestoneIds.add("M001");

	    request.setMilestoneIds(milestoneIds);

	    // Act
	    ValidationException exception = assertThrows(
	            ValidationException.class,
	            () -> reportService.generateReport(request));

	    // Assert
	    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
	    assertEquals("Please select Phase first", exception.getMessage());

	    verifyNoInteractions(projectRepository);
	    verifyNoInteractions(mapper);
	}

	@Test
	void generateReport_WhenTaskSelectedWithoutMilestone_ShouldThrowValidationException() {

	    // Arrange
	    GenerateReportModel request = new GenerateReportModel();

	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPhaseId("PH001");
	    request.setTaskId("T001");

	    // Act
	    ValidationException exception = assertThrows(
	            ValidationException.class,
	            () -> reportService.generateReport(request));

	    // Assert
	    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
	    assertEquals("Please select Milestone before Task", exception.getMessage());

	    verifyNoInteractions(projectRepository);
	    verifyNoInteractions(mapper);
	}

	@Test
	void generateReport_WhenSubTaskSelectedWithoutTask_ShouldThrowValidationException() {

	    // Arrange
	    GenerateReportModel request = new GenerateReportModel();

	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPhaseId("PH001");

	    List<String> milestoneIds = new ArrayList<>();
	    milestoneIds.add("M001");
	    request.setMilestoneIds(milestoneIds);

	    request.setSubTaskId("ST001");

	    // Act
	    ValidationException exception = assertThrows(
	            ValidationException.class,
	            () -> reportService.generateReport(request));

	    // Assert
	    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
	    assertEquals("Please select Task before Subtask", exception.getMessage());

	    verifyNoInteractions(projectRepository);
	    verifyNoInteractions(mapper);
	}
	@Test
	void generateReport_WhenActivitySelectedWithoutSubTask_ShouldThrowValidationException() {

	    // Arrange
	    GenerateReportModel request = new GenerateReportModel();

	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPhaseId("PH001");

	    request.setMilestoneIds(List.of("M001"));

	    request.setTaskId("T001");

	    // Activity selected but SubTask not selected
	    request.setActivityId("ACT001");

	    // Act
	    ValidationException exception = assertThrows(
	            ValidationException.class,
	            () -> reportService.generateReport(request));

	    // Assert
	    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
	    assertEquals("Please select Subtask before Activity", exception.getMessage());

	    verifyNoInteractions(projectRepository);
	    verifyNoInteractions(mapper);
	}
	@Test
	void generateReport_WhenProjectNotFound_ShouldThrowResourceNotFoundException() {

		GenerateReportModel request = new GenerateReportModel();

		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		when(projectRepository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> reportService.generateReport(request));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.getErrorCode());

		verify(projectRepository).findById("P001");

		verifyNoInteractions(mapper);

		verifyNoMoreInteractions(projectRepository);
	}

	@Test
	void generateReport_WhenNoReportDataFound_ShouldThrowResourceNotFoundException() {

	    GenerateReportModel request = new GenerateReportModel();

	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPhaseId("INVALID_PHASE");

	    Activity activity = new Activity();
	    activity.setActivityId("ACT001");
	    activity.setActivityName("Activity-1");

	    Subtask subtask = new Subtask();
	    subtask.setSubTaskId("ST001");
	    subtask.setSubTaskName("SubTask-1");
	    subtask.setActivities(List.of(activity));

	    Task task = new Task();
	    task.setTaskId("T001");
	    task.setTaskName("Task-1");
	    task.setSubTasks(List.of(subtask));

	    Milestone milestone = new Milestone();
	    milestone.setMilestoneId("M001");
	    milestone.setMilestoneName("Milestone-1");
	    milestone.setTasks(List.of(task));

	    Phase phase = new Phase();
	    phase.setPhaseId("PH001");
	    phase.setPhaseName("Phase-1");
	    phase.setMilestones(List.of(milestone));

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo Project");
	    project.setPhases(List.of(phase));

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    ResourceNotFoundException exception = assertThrows(
	            ResourceNotFoundException.class,
	            () -> reportService.generateReport(request));

	    assertEquals(ErrorCode.NO_REPORT_DATA_FOUND, exception.getErrorCode());

	    verify(projectRepository).findById("P001");
	    verifyNoInteractions(mapper);
	    verifyNoMoreInteractions(projectRepository);
	}

	@Test
	void generateReport_WhenValidRequest_ShouldReturnActivityListSuccessfully() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("P001");
		request.setProjectName("Demo Project");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");
		activity.setExecutionStatus("Completed");
		activity.setPlannedStartDate(LocalDate.of(2025, 1, 10));
		activity.setPlannedEndDate(LocalDate.of(2025, 1, 20));

		ActivityModel activityModel = new ActivityModel();
		activityModel.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(List.of(activity));

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(List.of(subtask));

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(List.of(task));

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(List.of(milestone));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(Project.class), any(Phase.class), any(Milestone.class), any(Task.class),
				any(Subtask.class), any(Activity.class))).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("Activity-1", result.get(0).getActivityName());

		verify(projectRepository).findById("P001");

		verify(mapper, times(1)).toActivityModel(any(Project.class), any(Phase.class), any(Milestone.class),
				any(Task.class), any(Subtask.class), any(Activity.class));

		verifyNoMoreInteractions(projectRepository);
	}

	@Test
	void generateReport_WhenExecutionStatusFilterMatches_ShouldReturnFilteredActivities() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setExecutionStatus("Completed");

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");
		activity.setExecutionStatus("Completed");

		ActivityModel activityModel = new ActivityModel();
		activityModel.setActivityName("Activity-1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(List.of(activity));

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(List.of(subtask));

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(List.of(task));

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(List.of(milestone));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(Project.class), any(Phase.class), any(Milestone.class), any(Task.class),
				any(Subtask.class), any(Activity.class))).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("Activity-1", result.get(0).getActivityName());

		verify(projectRepository).findById("P001");

		verify(mapper, times(1)).toActivityModel(any(Project.class), any(Phase.class), any(Milestone.class),
				any(Task.class), any(Subtask.class), any(Activity.class));

		verifyNoMoreInteractions(projectRepository);
	}

	@Test
	void generateReport_WhenPhaseFilterMatches_ShouldReturnFilteredActivities() {

	    GenerateReportModel request = new GenerateReportModel();
	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPhaseId("PH001");

	    Activity activity = new Activity();
	    activity.setActivityId("ACT001");
	    activity.setActivityName("Activity-1");

	    ActivityModel activityModel = new ActivityModel();
	    activityModel.setActivityName("Activity-1");

	    Subtask subtask = new Subtask();
	    subtask.setSubTaskId("ST001");
	    subtask.setSubTaskName("SubTask-1");
	    subtask.setActivities(List.of(activity));

	    Task task = new Task();
	    task.setTaskId("T001");
	    task.setTaskName("Task-1");
	    task.setSubTasks(List.of(subtask));

	    Milestone milestone = new Milestone();
	    milestone.setMilestoneId("M001");
	    milestone.setMilestoneName("Milestone-1");
	    milestone.setTasks(List.of(task));

	    Phase phase1 = new Phase();
	    phase1.setPhaseId("PH001");
	    phase1.setPhaseName("Phase-1");
	    phase1.setMilestones(List.of(milestone));

	    Phase phase2 = new Phase();
	    phase2.setPhaseId("PH002");
	    phase2.setPhaseName("Phase-2");
	    phase2.setMilestones(new ArrayList<>());

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo Project");
	    project.setPhases(List.of(phase1, phase2));

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    when(mapper.toActivityModel(any(), any(), any(), any(), any(), any()))
	            .thenReturn(activityModel);

	    List<ActivityModel> result = reportService.generateReport(request);

	    assertNotNull(result);
	    assertEquals(1, result.size());

	    verify(projectRepository).findById("P001");
	    verify(mapper, times(1))
	            .toActivityModel(any(), any(), any(), any(), any(), any());
	}
	@Test
	void generateReport_WhenMilestoneFilterMatches_ShouldReturnFilteredActivities() {

	    GenerateReportModel request = new GenerateReportModel();
	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPhaseId("PH001");
	    request.setMilestoneIds(List.of("M001"));

	    Activity activity = new Activity();
	    activity.setActivityId("ACT001");
	    activity.setActivityName("Activity-1");

	    ActivityModel activityModel = new ActivityModel();

	    Subtask subtask = new Subtask();
	    subtask.setSubTaskId("ST001");
	    subtask.setSubTaskName("SubTask-1");
	    subtask.setActivities(List.of(activity));

	    Task task = new Task();
	    task.setTaskId("T001");
	    task.setTaskName("Task-1");
	    task.setSubTasks(List.of(subtask));

	    Milestone milestone1 = new Milestone();
	    milestone1.setMilestoneId("M001");
	    milestone1.setMilestoneName("Milestone-1");
	    milestone1.setTasks(List.of(task));

	    Milestone milestone2 = new Milestone();
	    milestone2.setMilestoneId("M002");
	    milestone2.setMilestoneName("Milestone-2");
	    milestone2.setTasks(new ArrayList<>());

	    Phase phase = new Phase();
	    phase.setPhaseId("PH001");
	    phase.setPhaseName("Phase-1");
	    phase.setMilestones(List.of(milestone1, milestone2));

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo Project");
	    project.setPhases(List.of(phase));

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    when(mapper.toActivityModel(any(), any(), any(), any(), any(), any()))
	            .thenReturn(activityModel);

	    List<ActivityModel> result = reportService.generateReport(request);

	    assertNotNull(result);
	    assertEquals(1, result.size());

	    verify(projectRepository).findById("P001");
	    verify(mapper, times(1))
	            .toActivityModel(any(), any(), any(), any(), any(), any());
	}
	@Test
	void generateReport_WhenTaskFilterMatches_ShouldReturnFilteredActivities() {

	    GenerateReportModel request = new GenerateReportModel();
	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPhaseId("PH001");
	    request.setMilestoneIds(List.of("M001"));
	    request.setTaskId("T001");

	    Activity activity = new Activity();
	    activity.setActivityId("ACT001");
	    activity.setActivityName("Activity-1");

	    ActivityModel activityModel = new ActivityModel();

	    Subtask subtask = new Subtask();
	    subtask.setSubTaskId("ST001");
	    subtask.setSubTaskName("SubTask-1");
	    subtask.setActivities(List.of(activity));

	    Task task1 = new Task();
	    task1.setTaskId("T001");
	    task1.setTaskName("Task-1");
	    task1.setSubTasks(List.of(subtask));

	    Task task2 = new Task();
	    task2.setTaskId("T002");
	    task2.setTaskName("Task-2");
	    task2.setSubTasks(new ArrayList<>());

	    Milestone milestone = new Milestone();
	    milestone.setMilestoneId("M001");
	    milestone.setMilestoneName("Milestone-1");
	    milestone.setTasks(List.of(task1, task2));

	    Phase phase = new Phase();
	    phase.setPhaseId("PH001");
	    phase.setPhaseName("Phase-1");
	    phase.setMilestones(List.of(milestone));

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo Project");
	    project.setPhases(List.of(phase));

	    when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

	    when(mapper.toActivityModel(any(), any(), any(), any(), any(), any()))
	            .thenReturn(activityModel);

	    List<ActivityModel> result = reportService.generateReport(request);

	    assertNotNull(result);
	    assertEquals(1, result.size());

	    verify(projectRepository).findById("P001");
	    verify(mapper, times(1)).toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_WhenSubTaskFilterMatches_ShouldReturnFilteredActivities() {

	    GenerateReportModel request = new GenerateReportModel();
	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPhaseId("PH001");
	    request.setMilestoneIds(List.of("M001"));
	    request.setTaskId("T001");
	    request.setSubTaskId("ST001");

	    Activity activity = new Activity();
	    activity.setActivityId("ACT001");
	    activity.setActivityName("Activity-1");

	    ActivityModel activityModel = new ActivityModel();
	    activityModel.setActivityName("Activity-1");

	    Subtask subTask1 = new Subtask();
	    subTask1.setSubTaskId("ST001");
	    subTask1.setSubTaskName("SubTask-1");
	    subTask1.setActivities(List.of(activity));

	    Subtask subTask2 = new Subtask();
	    subTask2.setSubTaskId("ST002");
	    subTask2.setSubTaskName("SubTask-2");
	    subTask2.setActivities(new ArrayList<>());

	    Task task = new Task();
	    task.setTaskId("T001");
	    task.setTaskName("Task-1");
	    task.setSubTasks(List.of(subTask1, subTask2));

	    Milestone milestone = new Milestone();
	    milestone.setMilestoneId("M001");
	    milestone.setMilestoneName("Milestone-1");
	    milestone.setTasks(List.of(task));

	    Phase phase = new Phase();
	    phase.setPhaseId("PH001");
	    phase.setPhaseName("Phase-1");
	    phase.setMilestones(List.of(milestone));

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo Project");
	    project.setPhases(List.of(phase));

	    when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

	    when(mapper.toActivityModel(any(), any(), any(), any(), any(), any()))
	            .thenReturn(activityModel);

	    List<ActivityModel> result = reportService.generateReport(request);

	    assertNotNull(result);
	    assertEquals(1, result.size());

	    verify(projectRepository).findById("P001");
	    verify(mapper, times(1))
	            .toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_WhenDateRangeMatches_ShouldReturnFilteredActivities() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPlannedStartDate(LocalDate.of(2025, 1, 1));
		request.setPlannedEndDate(LocalDate.of(2025, 12, 31));

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");
		activity.setPlannedStartDate(LocalDate.of(2025, 5, 1));
		activity.setPlannedEndDate(LocalDate.of(2025, 5, 31));

		ActivityModel activityModel = new ActivityModel();

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(List.of(activity));

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(List.of(subtask));

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(List.of(task));

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(List.of(milestone));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(), any(), any(), any(), any(), any())).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertNotNull(result);
		assertEquals(1, result.size());

		verify(mapper).toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_WhenActivityIsOutsideDateRange_ShouldThrowResourceNotFoundException() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("P001");
		request.setProjectName("Demo Project");
		request.setPlannedStartDate(LocalDate.of(2025, 1, 1));
		request.setPlannedEndDate(LocalDate.of(2025, 12, 31));

		Activity activity = new Activity();
		activity.setActivityName("Activity-1");
		activity.setPlannedStartDate(LocalDate.of(2026, 1, 1));
		activity.setPlannedEndDate(LocalDate.of(2026, 1, 31));

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("SubTask-1");
		subtask.setActivities(List.of(activity));

		Task task = new Task();
		task.setTaskName("Task-1");
		task.setSubTasks(List.of(subtask));

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone-1");
		milestone.setTasks(List.of(task));

		Phase phase = new Phase();
		phase.setPhaseName("Phase-1");
		phase.setMilestones(List.of(milestone));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> reportService.generateReport(request));

		assertEquals(ErrorCode.NO_REPORT_DATA_FOUND, exception.getErrorCode());

		verify(mapper, never()).toActivityModel(any(), any(), any(), any(), any(), any());
	}
	
	@Test
	void generateReport_WhenActivityHasNullPlannedDates_ShouldThrowResourceNotFoundException() {

	    GenerateReportModel request = new GenerateReportModel();
	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");
	    request.setPlannedStartDate(LocalDate.of(2025, 1, 1));
	    request.setPlannedEndDate(LocalDate.of(2025, 12, 31));

	    Activity activity = new Activity();
	    activity.setActivityName("Activity-1");
	    activity.setPlannedStartDate(null);
	    activity.setPlannedEndDate(null);

	    Subtask subtask = new Subtask();
	    subtask.setSubTaskName("SubTask-1");
	    subtask.setActivities(List.of(activity));

	    Task task = new Task();
	    task.setTaskName("Task-1");
	    task.setSubTasks(List.of(subtask));

	    Milestone milestone = new Milestone();
	    milestone.setMilestoneName("Milestone-1");
	    milestone.setTasks(List.of(task));

	    Phase phase = new Phase();
	    phase.setPhaseName("Phase-1");
	    phase.setMilestones(List.of(milestone));

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo Project");
	    project.setPhases(List.of(phase));

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    ResourceNotFoundException exception = assertThrows(
	            ResourceNotFoundException.class,
	            () -> reportService.generateReport(request));

	    assertEquals(ErrorCode.NO_REPORT_DATA_FOUND, exception.getErrorCode());

	    verify(mapper, never())
	            .toActivityModel(any(), any(), any(), any(), any(), any());
	}
	
	@Test
	void generateReport_WhenMultipleActivitiesExist_ShouldReturnAllActivities() {

	    GenerateReportModel request = new GenerateReportModel();
	    request.setProjectId("P001");
	    request.setProjectName("Demo Project");

	    Activity activity1 = new Activity();
	    activity1.setActivityName("Activity-1");

	    Activity activity2 = new Activity();
	    activity2.setActivityName("Activity-2");

	    ActivityModel model1 = new ActivityModel();
	    model1.setActivityName("Activity-1");

	    ActivityModel model2 = new ActivityModel();
	    model2.setActivityName("Activity-2");

	    Subtask subtask = new Subtask();
	    subtask.setSubTaskName("SubTask-1");
	    subtask.setActivities(List.of(activity1, activity2));

	    Task task = new Task();
	    task.setTaskName("Task-1");
	    task.setSubTasks(List.of(subtask));

	    Milestone milestone = new Milestone();
	    milestone.setMilestoneName("Milestone-1");
	    milestone.setTasks(List.of(task));

	    Phase phase = new Phase();
	    phase.setPhaseName("Phase-1");
	    phase.setMilestones(List.of(milestone));

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo Project");
	    project.setPhases(List.of(phase));

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    when(mapper.toActivityModel(any(), any(), any(), any(), any(), eq(activity1)))
	            .thenReturn(model1);

	    when(mapper.toActivityModel(any(), any(), any(), any(), any(), eq(activity2)))
	            .thenReturn(model2);

	    List<ActivityModel> result = reportService.generateReport(request);

	    assertEquals(2, result.size());

	    verify(mapper, times(2))
	            .toActivityModel(any(), any(), any(), any(), any(), any());
	}

}