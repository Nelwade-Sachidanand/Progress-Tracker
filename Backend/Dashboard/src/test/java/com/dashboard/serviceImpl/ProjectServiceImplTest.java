package com.dashboard.serviceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.dashboard.repository.ProjectRepository;
import com.dashboard.repository.UserRepository;
import com.dashboard.service.AuditService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Project;
import com.dashboard.entity.User;
import com.dashboard.exception.DatabaseException;

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

	@Test
	void deleteProject_Success() {

		// Arrange
		mockLoggedInUser();

		String projectName = "Progress Tracke";

		Project project = new Project();

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = mock(Response.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findByProjectName(projectName)).thenReturn(Optional.of(project));

		when(userRepository.findByProjectNamesContaining(projectName)).thenReturn(new ArrayList<>());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		// Act
		Response result = projectService.deleteProject(projectName);

		// Assert
		assertNotNull(result);

		verify(projectRepository).delete(project);

		verify(auditService).saveAuditLog(anyString(), anyString(), anyString(), anyString(), any(), any(),
				anyString());
	}

	@Test
	void shouldGetAllProjectsSuccessfully() {

		// Arrange
		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response expectedResponse = new Response();

		List<Project> projects = List.of(new Project(), new Project());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findAll()).thenReturn(projects);

		when(responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Projects fetched successfully", projects)).thenReturn(expectedResponse);

		// Act
		Response actualResponse = projectService.getAllProjects();

		// Assert
		assertNotNull(actualResponse);

		assertEquals(expectedResponse, actualResponse);

		verify(projectRepository).findAll();
	}

	@Test
	void shouldThrowDatabaseExceptionWhenRepositoryFails() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findAll()).thenThrow(new RuntimeException("Database Error"));

		DatabaseException exception = assertThrows(DatabaseException.class, () -> projectService.getAllProjects());

		assertEquals("Unable to fetch projects", exception.getMessage());
	}

}