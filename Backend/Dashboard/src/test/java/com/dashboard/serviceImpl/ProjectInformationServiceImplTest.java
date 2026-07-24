package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.modelmapper.ModelMapper;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.ProjectInformation;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.ApplicationException;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.ProjectInformationModel;
import com.novillex.progresstracker.repository.ProjectInformationRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.serviceImpl.ProjectInformationServiceImpl;
import com.novillex.progresstracker.util.UserContextUtil;

@ExtendWith(MockitoExtension.class)
public class ProjectInformationServiceImplTest {

	@Mock
	private ProjectInformationRepository repository;

	@Mock
	private ApplicationContext context;

	@Mock
	private ResponseBuilder responseBuilder;

	@Mock
	private AuditService auditService;

	@Mock
	private ModelMapper modelMapper;
	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private ProjectInformationServiceImpl service;

	@BeforeEach
	void setup() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testUser", null));

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
	}

	@Test
	void shouldCreateProjectInformationSuccessfully() {

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("Tracker");
		model.setBankName("HDFC");
		model.setProjectManager("Manager");

		ProjectInformation project = new ProjectInformation();
		project.setId("PI001");
		project.setProjectName("Tracker");
		project.setBankName("HDFC");
		project.setProjectManager("Manager");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findByProjectNameAndBankName("Tracker", "HDFC")).thenReturn(Optional.empty());

		when(modelMapper.map(model, ProjectInformation.class)).thenReturn(project);

		when(repository.save(any(ProjectInformation.class))).thenReturn(project);

		when(projectRepository.findByProjectInformationId("PI001")).thenReturn(Optional.empty());

		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
			Project dashboard = invocation.getArgument(0);
			dashboard.setId("DASH001");
			return dashboard;
		});

		User admin = new User();
		admin.setId("ADMIN001");
		admin.setRole("ADMIN");
		admin.setProjectIds(new ArrayList<>());

		when(userRepository.findByRole("ADMIN")).thenReturn(List.of(admin));

		when(userRepository.saveAll(anyList())).thenReturn(List.of(admin));

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			Response result = service.createProjectInformation(model);

			assertEquals(response, result);

			verify(repository).save(any(ProjectInformation.class));
			verify(projectRepository).save(any(Project.class));
			verify(userRepository).findByRole("ADMIN");
			verify(userRepository).saveAll(anyList());

			verify(auditService, times(2)).saveAuditLog(any(), any(), any(), any(), any(), any(), eq("testUser"));
		}
	}

	@Test
	void shouldGetAllProjectInformationSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		List<ProjectInformation> projects = List.of(new ProjectInformation(), new ProjectInformation());

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findAll()).thenReturn(projects);

		when(responseBuilder.createResponse(any(), any(), anyString(), eq(projects))).thenReturn(response);

		Response result = service.getAllProjectInformation();

		assertEquals(response, result);

		verify(repository).findAll();

		verify(responseBuilder).createResponse(any(), any(), eq("Project information fetched successfully"),
				eq(projects));
	}

	@Test
	void shouldGetProjectInformationByIdSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		ProjectInformation project = new ProjectInformation();
		project.setId("P001");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), eq(project))).thenReturn(response);

		Response result = service.getProjectInformationById("P001");

		assertEquals(response, result);

		verify(repository).findById("P001");

		verify(responseBuilder).createResponse(any(), any(), eq("Project information fetched successfully"),
				eq(project));
	}

	@Test
	void shouldThrowProjectNotFoundForGetById() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
				() -> service.getProjectInformationById("P001"));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, ex.getErrorCode());

		verify(repository).findById("P001");

		verify(responseBuilder, never()).createResponse(any(), any(), anyString(), any());
	}

	@Test
	void shouldUpdateProjectInformationSuccessfully() throws Exception {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		ProjectInformation existing = new ProjectInformation();
		existing.setId("P001");
		existing.setProjectName("Old");

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("New");

		ProjectInformation oldProject = new ProjectInformation();
		oldProject.setId("P001");
		oldProject.setProjectName("Old");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findById("P001")).thenReturn(Optional.of(existing));

		when(objectMapper.writeValueAsString(any(ProjectInformation.class))).thenReturn("json");

		when(objectMapper.readValue("json", ProjectInformation.class)).thenReturn(oldProject);

		// Simulate ModelMapper updating the entity
		doAnswer(invocation -> {
			ProjectInformationModel source = invocation.getArgument(0);
			ProjectInformation destination = invocation.getArgument(1);
			destination.setProjectName(source.getProjectName());
			return destination;
		}).when(modelMapper).map(any(ProjectInformationModel.class), any(ProjectInformation.class));

		// Force change detection
		when(objectMapper.valueToTree(oldProject)).thenReturn(new ObjectMapper().createObjectNode());

		when(objectMapper.valueToTree(existing)).thenReturn(new ObjectMapper().createArrayNode());

		when(repository.save(any(ProjectInformation.class))).thenReturn(existing);

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			Response result = service.updateProjectInformation("P001", model);

			assertEquals(response, result);

			verify(repository).findById("P001");
			verify(repository).save(any(ProjectInformation.class));

			verify(auditService).saveAuditLog(eq(AuditAction.UPDATE_PROJECT_INFORMATION), eq(AuditEntity.PROJECT),
					anyString(), anyString(), any(), any(), eq("testUser"));
		}
	}

	@Test
	void shouldThrowProjectNotFoundForUpdate() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ProjectInformationModel model = new ProjectInformationModel();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
				() -> service.updateProjectInformation("P001", model));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, ex.getErrorCode());

		verify(repository).findById("P001");

		verify(repository, never()).save(any(ProjectInformation.class));

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void shouldDeleteProjectInformationSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		ProjectInformation project = new ProjectInformation();
		project.setId("P001");
		project.setProjectName("Tracker");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findById("P001")).thenReturn(Optional.of(project));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			Response result = service.deleteProjectInformation("P001");

			assertEquals(response, result);

			verify(repository).delete(project);

			verify(auditService).saveAuditLog(eq(AuditAction.DELETE_PROJECT_INFORMATION), eq(AuditEntity.PROJECT),
					eq("Tracker"), eq("Tracker"), eq(project), isNull(), eq("testUser"));
		}
	}

	@Test
	void shouldThrowProjectNotFoundForDelete() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
				() -> service.deleteProjectInformation("P001"));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, ex.getErrorCode());

		verify(repository).findById("P001");

		verify(repository, never()).delete(any());

		verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void shouldReturnEmptyListWhenNoProjectsExist() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findAll()).thenReturn(List.of());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = service.getAllProjectInformation();

		assertEquals(response, result);

		verify(repository).findAll();

		verify(responseBuilder).createResponse(any(), any(), eq("Project information fetched successfully"),
				eq(List.of()));
	}

	@Test
	void shouldThrowExceptionWhenRepositoryFailsDuringCreate() {

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("Tracker");
		model.setBankName("HDFC");
		model.setProjectManager("Manager");

		ProjectInformation project = new ProjectInformation();
		project.setProjectName("Tracker");
		project.setBankName("HDFC");
		project.setProjectManager("Manager");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findByProjectNameAndBankName("Tracker", "HDFC")).thenReturn(Optional.empty());

		when(modelMapper.map(model, ProjectInformation.class)).thenReturn(project);

		when(repository.save(any(ProjectInformation.class))).thenThrow(new RuntimeException("DB error"));

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			ApplicationException ex = assertThrows(ApplicationException.class,
					() -> service.createProjectInformation(model));

			assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());

			verify(repository).findByProjectNameAndBankName("Tracker", "HDFC");
			verify(repository).save(any(ProjectInformation.class));

			verify(projectRepository, never()).findByProjectInformationId(anyString());
			verify(projectRepository, never()).save(any(Project.class));

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void shouldThrowExceptionWhenUpdateFails() throws Exception {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ProjectInformation existing = new ProjectInformation();
		existing.setId("P001");
		existing.setProjectName("Tracker");

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("Updated Tracker");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findById("P001")).thenReturn(Optional.of(existing));

		when(objectMapper.writeValueAsString(any(ProjectInformation.class))).thenReturn("json");

		ProjectInformation oldProject = new ProjectInformation();
		oldProject.setId("P001");
		oldProject.setProjectName("Tracker");

		when(objectMapper.readValue("json", ProjectInformation.class)).thenReturn(oldProject);

		// Simulate ModelMapper updating the entity
		doAnswer(invocation -> {
			ProjectInformationModel source = invocation.getArgument(0);
			ProjectInformation destination = invocation.getArgument(1);
			destination.setProjectName(source.getProjectName());
			return destination;
		}).when(modelMapper).map(any(ProjectInformationModel.class), any(ProjectInformation.class));

		// Force the "changes found" branch
		when(objectMapper.valueToTree(oldProject)).thenReturn(new ObjectMapper().createObjectNode());

		when(objectMapper.valueToTree(existing)).thenReturn(new ObjectMapper().createArrayNode());

		when(repository.save(any(ProjectInformation.class))).thenThrow(new RuntimeException("Database error"));

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			DatabaseException ex = assertThrows(DatabaseException.class,
					() -> service.updateProjectInformation("P001", model));

			assertEquals(ErrorCode.DATABASE_ERROR, ex.getErrorCode());

			verify(repository).findById("P001");
			verify(repository).save(any(ProjectInformation.class));

			verify(auditService, never()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void shouldThrowExceptionWhenDeleteFails() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		ProjectInformation project = new ProjectInformation();
		project.setId("P001");
		project.setProjectName("Tracker");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findById("P001")).thenReturn(Optional.of(project));

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		doThrow(new RuntimeException("Delete failed")).when(repository).delete(project);

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			RuntimeException ex = assertThrows(RuntimeException.class, () -> service.deleteProjectInformation("P001"));

			assertEquals("Delete failed", ex.getMessage());

			verify(repository).findById("P001");
			verify(repository).delete(project);

			verify(auditService).saveAuditLog(eq(AuditAction.DELETE_PROJECT_INFORMATION), eq(AuditEntity.PROJECT),
					eq("Tracker"), eq("Tracker"), eq(project), isNull(), eq("testUser"));
		}
	}

	@Test
	void shouldAuditProjectCreation() {

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("Tracker");
		model.setBankName("HDFC");
		model.setProjectManager("Manager");

		ProjectInformation project = new ProjectInformation();
		project.setId("PI001");
		project.setProjectName("Tracker");
		project.setBankName("HDFC");
		project.setProjectManager("Manager");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findByProjectNameAndBankName("Tracker", "HDFC")).thenReturn(Optional.empty());

		when(modelMapper.map(model, ProjectInformation.class)).thenReturn(project);

		when(repository.save(any(ProjectInformation.class))).thenReturn(project);

		when(projectRepository.findByProjectInformationId("PI001")).thenReturn(Optional.empty());

		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
			Project dashboard = invocation.getArgument(0);
			dashboard.setId("DASH001");
			return dashboard;
		});

		User admin = new User();
		admin.setId("ADMIN001");
		admin.setRole("ADMIN");
		admin.setProjectIds(new ArrayList<>());

		when(userRepository.findByRole("ADMIN")).thenReturn(List.of(admin));

		when(userRepository.saveAll(anyList())).thenReturn(List.of(admin));

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			service.createProjectInformation(model);

			verify(auditService, times(2)).saveAuditLog(any(), any(), eq("Tracker"), eq("Tracker"), any(), any(),
					eq("testUser"));
		}
	}

	@Test
	void shouldThrowExceptionWhenModelMapperFails() {

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("Tracker");
		model.setBankName("HDFC");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findByProjectNameAndBankName("Tracker", "HDFC")).thenReturn(Optional.empty());

		when(modelMapper.map(eq(model), eq(ProjectInformation.class)))
				.thenThrow(new RuntimeException("Mapping failed"));

		ApplicationException ex = assertThrows(ApplicationException.class,
				() -> service.createProjectInformation(model));

		assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());

		verify(repository).findByProjectNameAndBankName("Tracker", "HDFC");

		verify(repository, never()).save(any(ProjectInformation.class));
	}

	@Test
    void shouldThrowExceptionWhenGetAllFails() {

        when(repository.findAll())
                .thenThrow(
                        new RuntimeException("Database error"));

        assertThrows(
                RuntimeException.class,
                () -> service.getAllProjectInformation()
        );
    }

	@Test
    void shouldThrowExceptionWhenGetByIdFails() {

        when(repository.findById("P001"))
                .thenThrow(
                        new RuntimeException("Database error"));

        assertThrows(
                RuntimeException.class,
                () -> service.getProjectInformationById("P001")
        );
    }

	@Test
	void shouldThrowExceptionWhenAuditFailsDuringCreate() {

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("Tracker");
		model.setBankName("HDFC");

		ProjectInformation project = new ProjectInformation();
		project.setProjectName("Tracker");
		project.setBankName("HDFC");

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findByProjectNameAndBankName("Tracker", "HDFC")).thenReturn(Optional.empty());

		when(modelMapper.map(any(ProjectInformationModel.class), eq(ProjectInformation.class))).thenReturn(project);

		when(repository.save(any(ProjectInformation.class))).thenReturn(project);

		when(projectRepository.findByProjectInformationId(anyString())).thenReturn(Optional.of(new Project())); // skip
																												// dashboard
																												// creation

		doThrow(new RuntimeException("Audit failed")).when(auditService).saveAuditLog(any(), any(), anyString(),
				anyString(), any(), any(), anyString());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			ApplicationException ex = assertThrows(ApplicationException.class,
					() -> service.createProjectInformation(model));

			assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
		}
	}

	@Test
	void shouldSetDefaultFieldsDuringCreate() {

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("Tracker");
		model.setBankName("HDFC");
		model.setProjectManager("Manager");

		ProjectInformation project = new ProjectInformation();
		project.setId("PI001");
		project.setProjectName("Tracker");
		project.setBankName("HDFC");
		project.setProjectManager("Manager");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(repository.findByProjectNameAndBankName("Tracker", "HDFC")).thenReturn(Optional.empty());

		when(modelMapper.map(model, ProjectInformation.class)).thenReturn(project);

		when(repository.save(any(ProjectInformation.class))).thenReturn(project);

		when(projectRepository.findByProjectInformationId("PI001")).thenReturn(Optional.empty());

		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
			Project p = invocation.getArgument(0);
			p.setId("DASH001");
			return p;
		});

		User admin = new User();
		admin.setId("ADMIN001");
		admin.setRole("ADMIN");
		admin.setProjectIds(new ArrayList<>());

		when(userRepository.findByRole("ADMIN")).thenReturn(List.of(admin));

		when(userRepository.saveAll(anyList())).thenReturn(List.of(admin));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		doNothing().when(auditService).saveAuditLog(any(), any(), any(), any(), any(), any(), any());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			Response result = service.createProjectInformation(model);

			assertNotNull(result);

			assertEquals("ACTIVE", project.getStatus());
			assertEquals("testUser", project.getCreatedBy());
			assertEquals("testUser", project.getUpdatedBy());

			assertNotNull(project.getCreatedAt());
			assertNotNull(project.getUpdatedAt());

			verify(repository).save(project);
			verify(projectRepository).save(any(Project.class));
			verify(userRepository).findByRole("ADMIN");
			verify(userRepository).saveAll(anyList());

			verify(auditService, times(2)).saveAuditLog(any(), any(), any(), any(), any(), any(), eq("testUser"));
		}
	}

	@Test
	void shouldSetUpdatedTimeDuringUpdate() throws Exception {

		ProjectInformation existing = new ProjectInformation();
		existing.setId("P001");
		existing.setProjectName("Tracker");

		ProjectInformationModel model = new ProjectInformationModel();
		model.setProjectName("Tracker Updated");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);
		when(repository.findById("P001")).thenReturn(Optional.of(existing));

		when(objectMapper.writeValueAsString(any(ProjectInformation.class))).thenReturn("json");

		ProjectInformation oldProject = new ProjectInformation();
		oldProject.setId("P001");
		oldProject.setProjectName("Tracker");

		when(objectMapper.readValue("json", ProjectInformation.class)).thenReturn(oldProject);

		// Simulate the model mapper updating the entity
		doAnswer(invocation -> {
			ProjectInformationModel src = invocation.getArgument(0);
			ProjectInformation dest = invocation.getArgument(1);
			dest.setProjectName(src.getProjectName());
			return dest;
		}).when(modelMapper).map(any(ProjectInformationModel.class), any(ProjectInformation.class));

		// Force the "changes detected" branch
		when(objectMapper.valueToTree(oldProject)).thenReturn(new ObjectMapper().createObjectNode());
		when(objectMapper.valueToTree(existing)).thenReturn(new ObjectMapper().createArrayNode());

		when(repository.save(any(ProjectInformation.class))).thenReturn(existing);
		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

		try (MockedStatic<UserContextUtil> mocked = mockStatic(UserContextUtil.class)) {

			mocked.when(UserContextUtil::getCurrentUser).thenReturn("testUser");

			service.updateProjectInformation("P001", model);

			assertNotNull(existing.getUpdatedAt());
			verify(repository).save(existing);
		}
	}
}