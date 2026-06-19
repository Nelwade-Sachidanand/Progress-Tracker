package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
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
	void generateReport_ProjectNotFound() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("P001");

		when(projectRepository.findById("P001")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> reportService.generateReport(request));

		verify(projectRepository).findById("P001");

		verifyNoInteractions(mapper);
	}

	@Test
	void generateReport_NoDataFound() {

		GenerateReportModel request = new GenerateReportModel();

		request.setProjectId("P001");
		request.setPhaseName("InvalidPhase");

		Activity activity = new Activity();

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("Sub1");
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

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		assertThrows(ResourceNotFoundException.class, () -> reportService.generateReport(request));

		verify(projectRepository).findById("P001");
	}

	@Test
	void generateReport_Success() {

		GenerateReportModel request = new GenerateReportModel();

		request.setProjectId("P001");

		Activity activity = new Activity();
		activity.setActivityName("Activity1");

		ActivityModel activityModel = new ActivityModel();
		activityModel.setActivityName("Activity1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("Sub1");
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

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(), any(), any(), any(), any(), any())).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertNotNull(result);

		assertEquals(1, result.size());

		verify(projectRepository).findById("P001");

		verify(mapper).toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_FilterByExecutionStatus() {

		GenerateReportModel request = new GenerateReportModel();

		request.setProjectId("P001");
		request.setExecutionStatus("Completed");

		Activity activity = new Activity();
		activity.setActivityName("Activity1");
		activity.setExecutionStatus("Completed");

		ActivityModel activityModel = new ActivityModel();
		activityModel.setActivityName("Activity1");

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("Sub1");
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
		project.setProjectName("Demo");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(), any(), any(), any(), any(), any())).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertEquals(1, result.size());

		verify(mapper, times(1)).toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_FilterByMilestone() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("P001");
		List<String> milestone=new ArrayList<String>();
		
		request.setMilestoneNames(milestone);

		Activity activity = new Activity();
		activity.setActivityName("Activity1");

		ActivityModel activityModel = new ActivityModel();

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("Sub1");
		subtask.setActivities(List.of(activity));

		Task task = new Task();
		task.setTaskName("Task1");
		task.setSubTasks(List.of(subtask));

		Milestone milestone1 = new Milestone();
		milestone1.setMilestoneName("Milestone1");
		milestone1.setTasks(List.of(task));

		Milestone milestone2 = new Milestone();
		milestone2.setMilestoneName("Milestone2");
		milestone2.setTasks(new ArrayList<>());

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");
		phase.setMilestones(List.of(milestone1, milestone2));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(), any(), any(), any(), any(), any())).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertEquals(1, result.size());

		verify(mapper, times(1)).toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_FilterByTask() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("P001");
		request.setTaskName("Task1");

		Activity activity = new Activity();
		activity.setActivityName("Activity1");

		ActivityModel activityModel = new ActivityModel();

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("Sub1");
		subtask.setActivities(List.of(activity));

		Task task1 = new Task();
		task1.setTaskName("Task1");
		task1.setSubTasks(List.of(subtask));

		Task task2 = new Task();
		task2.setTaskName("Task2");
		task2.setSubTasks(new ArrayList<>());

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(List.of(task1, task2));

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");
		phase.setMilestones(List.of(milestone));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(), any(), any(), any(), any(), any())).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertEquals(1, result.size());

		verify(mapper, times(1)).toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_FilterBySubTask() {

		GenerateReportModel request = new GenerateReportModel();
		request.setProjectId("P001");
		request.setSubtaskName("Sub1");

		Activity activity = new Activity();
		activity.setActivityName("Activity1");

		ActivityModel activityModel = new ActivityModel();

		Subtask subtask1 = new Subtask();
		subtask1.setSubTaskName("Sub1");
		subtask1.setActivities(List.of(activity));

		Subtask subtask2 = new Subtask();
		subtask2.setSubTaskName("Sub2");
		subtask2.setActivities(new ArrayList<>());

		Task task = new Task();
		task.setTaskName("Task1");
		task.setSubTasks(List.of(subtask1, subtask2));

		Milestone milestone = new Milestone();
		milestone.setMilestoneName("Milestone1");
		milestone.setTasks(List.of(task));

		Phase phase = new Phase();
		phase.setPhaseName("Phase1");
		phase.setMilestones(List.of(milestone));

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(), any(), any(), any(), any(), any())).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertEquals(1, result.size());

		verify(mapper, times(1)).toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_FilterByDateRange() {

		GenerateReportModel request = new GenerateReportModel();

		request.setProjectId("P001");
		request.setPlannedStartDate(LocalDate.of(2025, 1, 1));
		request.setPlannedEndDate(LocalDate.of(2025, 12, 31));

		Activity activity = new Activity();
		activity.setActivityName("Activity1");
		activity.setPlannedStartDate(LocalDate.of(2025, 5, 1));
		activity.setPlannedEndDate(LocalDate.of(2025, 5, 31));

		ActivityModel activityModel = new ActivityModel();

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("Sub1");
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
		project.setProjectName("Demo");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		when(mapper.toActivityModel(any(), any(), any(), any(), any(), any())).thenReturn(activityModel);

		List<ActivityModel> result = reportService.generateReport(request);

		assertEquals(1, result.size());

		verify(mapper, times(1)).toActivityModel(any(), any(), any(), any(), any(), any());
	}

	@Test
	void generateReport_ActivityWithoutDates() {

		GenerateReportModel request = new GenerateReportModel();

		request.setProjectId("P001");
		request.setPlannedStartDate(LocalDate.of(2025, 1, 1));
		request.setPlannedEndDate(LocalDate.of(2025, 12, 31));

		Activity activity = new Activity();
		activity.setActivityName("Activity1");
		activity.setPlannedStartDate(null);
		activity.setPlannedEndDate(null);

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("Sub1");
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
		project.setProjectName("Demo");
		project.setPhases(List.of(phase));

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		assertThrows(ResourceNotFoundException.class, () -> reportService.generateReport(request));

		verify(mapper, never()).toActivityModel(any(), any(), any(), any(), any(), any());
	}
}
