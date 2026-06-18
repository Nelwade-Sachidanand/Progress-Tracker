package com.dashboard.serviceImpl;

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

	/*
	 * @Test void deleteProject_ProjectNotFound() {
	 * 
	 * mockLoggedInUser();
	 * 
	 * ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
	 * 
	 * when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
	 * 
	 * when(projectRepository.findById("P001")).thenReturn(Optional.empty());
	 * 
	 * assertThrows(ResourceNotFoundException.class, () ->
	 * projectService.deleteProject("P001"));
	 * 
	 * verify(projectRepository, never()).delete(any()); verify(auditService,
	 * never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any()); }
	 * 
	 * @Test void deleteProject_ShouldRemoveProjectFromUsers() {
	 * 
	 * mockLoggedInUser();
	 * 
	 * Project project = new Project(); project.setId("P001");
	 * project.setProjectName("Progress Tracker");
	 * 
	 * User user = new User(); user.setProjectNames(new
	 * ArrayList<>(List.of("Progress Tracker", "ABC")));
	 * 
	 * ResponseBuilder responseBuilder = mock(ResponseBuilder.class); Response
	 * response = new Response();
	 * 
	 * when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
	 * 
	 * when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
	 * 
	 * when(userRepository.findByProjectNamesContaining("Progress Tracker")).
	 * thenReturn(List.of(user));
	 * 
	 * when(responseBuilder.createResponse(any(), any(), anyString(),
	 * any())).thenReturn(response);
	 * 
	 * Response result = projectService.deleteProject("P001");
	 * 
	 * assertNotNull(result);
	 * 
	 * assertFalse(user.getProjectNames().contains("Progress Tracker"));
	 * 
	 * verify(userRepository).saveAll(anyList());
	 * verify(projectRepository).delete(project); }
	 * 
	 * @Test void deleteProject_UserProjectNamesNull() {
	 * 
	 * mockLoggedInUser();
	 * 
	 * Project project = new Project(); project.setId("P001");
	 * project.setProjectName("Progress Tracker");
	 * 
	 * User user = new User(); user.setProjectNames(null);
	 * 
	 * ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
	 * 
	 * when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
	 * 
	 * when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
	 * 
	 * when(userRepository.findByProjectNamesContaining("Progress Tracker")).
	 * thenReturn(List.of(user));
	 * 
	 * when(responseBuilder.createResponse(any(), any(), anyString(),
	 * any())).thenReturn(new Response());
	 * 
	 * assertDoesNotThrow(() -> projectService.deleteProject("P001"));
	 * 
	 * verify(userRepository).saveAll(anyList()); }
	 * 
	 * @Test void deleteProject_AuditFailure() {
	 * 
	 * mockLoggedInUser();
	 * 
	 * Project project = new Project(); project.setId("P001");
	 * project.setProjectName("Progress Tracker");
	 * 
	 * ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
	 * 
	 * when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
	 * 
	 * when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
	 * 
	 * when(userRepository.findByProjectNamesContaining("Progress Tracker")).
	 * thenReturn(new ArrayList<>());
	 * 
	 * doThrow(new
	 * RuntimeException("Audit Error")).when(auditService).saveAuditLog(any(),
	 * any(), any(), any(), any(), any(), any());
	 * 
	 * DatabaseException exception = assertThrows(DatabaseException.class, () ->
	 * projectService.deleteProject("P001"));
	 * 
	 * assertNotNull(exception); }
	 * 
	 * @Test void deleteProject_DeleteFailure() {
	 * 
	 * mockLoggedInUser();
	 * 
	 * Project project = new Project(); project.setId("P001");
	 * project.setProjectName("Progress Tracker");
	 * 
	 * ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
	 * 
	 * when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
	 * 
	 * when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
	 * 
	 * when(userRepository.findByProjectNamesContaining("Progress Tracker")).
	 * thenReturn(new ArrayList<>());
	 * 
	 * doThrow(new
	 * RuntimeException("Delete Error")).when(projectRepository).delete(project);
	 * 
	 * DatabaseException exception = assertThrows(DatabaseException.class, () ->
	 * projectService.deleteProject("P001"));
	 * 
	 * assertNotNull(exception); }
	 * 
	 * @Test void deleteProject_SaveUsersFailure() {
	 * 
	 * mockLoggedInUser();
	 * 
	 * Project project = new Project(); project.setId("P001");
	 * project.setProjectName("Progress Tracker");
	 * 
	 * User user = new User(); user.setProjectNames(new
	 * ArrayList<>(List.of("Progress Tracker")));
	 * 
	 * ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
	 * 
	 * when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
	 * 
	 * when(projectRepository.findById("P001")).thenReturn(Optional.of(project));
	 * 
	 * when(userRepository.findByProjectNamesContaining("Progress Tracker")).
	 * thenReturn(List.of(user));
	 * 
	 * doThrow(new
	 * RuntimeException("DB Error")).when(userRepository).saveAll(anyList());
	 * 
	 * DatabaseException exception = assertThrows(DatabaseException.class, () ->
	 * projectService.deleteProject("P001"));
	 * 
	 * assertNotNull(exception); verify(projectRepository, never()).delete(any()); }
	 */
	@Test
	void shouldReturnEmptyProjectListSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response expectedResponse = new Response();

		List<Project> projects = new ArrayList<>();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(projectRepository.findAll()).thenReturn(projects);

		when(responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Projects fetched successfully", projects)).thenReturn(expectedResponse);

		Response actualResponse = projectService.getAllProjects();

		assertNotNull(actualResponse);

		verify(projectRepository).findAll();

		verify(responseBuilder).createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Projects fetched successfully", projects);
	}
}