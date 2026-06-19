package com.dashboard.serviceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.serviceImpl.ProjectServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

	@Mock
	private ApplicationContext context;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private ProjectServiceImpl projectService;

	private void mockLoggedInUser() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, new ArrayList<>()));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}
	
	
	@Test
	void deleteProject_ProjectNotFound() {

	    mockLoggedInUser();

	    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

	    when(context.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.empty());

	    assertThrows(
	            ResourceNotFoundException.class,
	            () -> projectService.deleteProject("P001"));

	    verify(projectRepository, never()).delete(any());
	}
	
	@Test
	void deleteProject_Success() {

	    mockLoggedInUser();

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo");

	    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
	    Response response = mock(Response.class);

	    when(context.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    when(userRepository.findByProjectIdsContaining("P001"))
	            .thenReturn(new ArrayList<>());

	    when(responseBuilder.createResponse(
	            any(),
	            any(),
	            anyString(),
	            any()))
	            .thenReturn(response);

	    Response result =
	            projectService.deleteProject("P001");

	    assertNotNull(result);

	    verify(projectRepository).delete(project);

	    verify(auditService).saveAuditLog(
	            any(),
	            any(),
	            any(),
	            any(),
	            any(),
	            any(),
	            any());
	}
	
	
	@Test
	void deleteProject_RemoveProjectFromUsers() {

	    mockLoggedInUser();

	    Project project = new Project();
	    project.setId("P001");
	    project.setProjectName("Demo");

	    User user = new User();
	    user.setProjectIds(
	            new ArrayList<>(List.of("P001", "P002")));

	    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

	    when(context.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    when(userRepository.findByProjectIdsContaining("P001"))
	            .thenReturn(List.of(user));

	    when(responseBuilder.createResponse(
	            any(),
	            any(),
	            anyString(),
	            any()))
	            .thenReturn(new Response());

	    projectService.deleteProject("P001");

	    assertFalse(
	            user.getProjectIds().contains("P001"));

	    verify(userRepository).saveAll(anyList());
	}
	
	@Test
	void deleteProject_SaveUsersFailure() {

	    mockLoggedInUser();

	    Project project = new Project();
	    project.setId("P001");

	    User user = new User();
	    user.setProjectIds(
	            new ArrayList<>(List.of("P001")));

	    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

	    when(context.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    when(userRepository.findByProjectIdsContaining("P001"))
	            .thenReturn(List.of(user));

	    doThrow(new RuntimeException())
	            .when(userRepository)
	            .saveAll(anyList());

	    assertThrows(
	            DatabaseException.class,
	            () -> projectService.deleteProject("P001"));
	}
	
	@Test
	void deleteProject_DeleteFailure() {

	    mockLoggedInUser();

	    Project project = new Project();
	    project.setId("P001");

	    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

	    when(context.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);

	    when(projectRepository.findById("P001"))
	            .thenReturn(Optional.of(project));

	    when(userRepository.findByProjectIdsContaining("P001"))
	            .thenReturn(new ArrayList<>());

	    doThrow(new RuntimeException())
	            .when(projectRepository)
	            .delete(project);

	    assertThrows(
	            DatabaseException.class,
	            () -> projectService.deleteProject("P001"));
	}
	
	@Test
	void getAllProjects_Success() {

	    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

	    List<Project> projects =
	            List.of(new Project(), new Project());

	    Response expectedResponse = new Response();

	    when(context.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);

	    when(projectRepository.findAll())
	            .thenReturn(projects);

	    when(responseBuilder.createResponse(
	            StatusCode.SUCCESS,
	            StatusCode.SUCCESS_STATUS_TYPE,
	            "Projects fetched successfully",
	            projects))
	            .thenReturn(expectedResponse);

	    Response result =
	            projectService.getAllProjects();

	    assertEquals(expectedResponse, result);
	}
	
	@Test
	void getAllProjects_DatabaseFailure() {

	    ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

	    when(context.getBean(ResponseBuilder.class))
	            .thenReturn(responseBuilder);

	    when(projectRepository.findAll())
	            .thenThrow(new RuntimeException());

	    assertThrows(
	            DatabaseException.class,
	            () -> projectService.getAllProjects());
	}
}