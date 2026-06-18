package com.dashboard.serviceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.service.AuditService;
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
	private ApplicationContext context;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private UpdateActivityServiceImpl updateActivityService;

	@Test
	void shouldThrowProjectNotFoundException() {

		ActivityModel request = new ActivityModel();

		request.setProjectName("Invalid Project");
		request.setPhaseName("Phase1");
		request.setMilestoneName("Milestone1");
		request.setTaskName("Task1");
		request.setSubTaskName("SubTask1");
		request.setActivityName("Activity1");

		when(projectRepository.findByProjectName("Invalid Project")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> updateActivityService.updateActivity(request));
	}

	@Test
	void shouldThrowActivityNotFoundException() {

		// Arrange
		ActivityModel request = new ActivityModel();

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

		// No activities added intentionally

		task.setSubTasks(List.of(subtask));
		milestone.setTasks(List.of(task));
		phase.setMilestones(List.of(milestone));
		project.setPhases(List.of(phase));

		when(projectRepository.findByProjectName("Progress Tracker")).thenReturn(Optional.of(project));

		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> updateActivityService.updateActivity(request));
	}
}