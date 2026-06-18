package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
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
	@Test
	void createStructure_ActivityAlreadyExists() {

	    ActivityModel request = new ActivityModel();

	    request.setProjectId("P001");
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

	    when(context.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    assertThrows(
	            ValidationException.class,
	            () -> createStructureService.createStructure(request));

	    verify(projectRepository, never())
	            .save(any());

	    verify(auditService, never())
	            .saveAuditLog(
	                    any(),
	                    any(),
	                    any(),
	                    any(),
	                    any(),
	                    any(),
	                    any());
	}

}
