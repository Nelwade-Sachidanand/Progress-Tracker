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

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.ProjectInformation;
import com.novillex.progresstracker.entity.User;
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

		when(repository.findByProjectName("Tracker")).thenReturn(Optional.empty());

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

		List<ProjectInformation> projects = List.of(new ProjectInformation(), new ProjectInformation());

		when(repository.findAll()).thenReturn(projects);

		service.getAllProjectInformation();

		verify(repository).findAll();
	}

	@Test
	void shouldGetProjectInformationByIdSuccessfully() {

		ProjectInformation project = new ProjectInformation();

		project.setId("P001");

		when(repository.findById("P001")).thenReturn(Optional.of(project));

		service.getProjectInformationById("P001");

		verify(repository).findById("P001");
	}

	@Test
    void shouldThrowProjectNotFoundForGetById() {

        when(repository.findById("P001"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> service.getProjectInformationById("P001"));

        assertEquals(
                ErrorCode.PROJECT_NOT_FOUND,
                ex.getErrorCode());
    }

	@Test
	void shouldUpdateProjectInformationSuccessfully() {

		ProjectInformation existing = new ProjectInformation();

		existing.setId("P001");
		existing.setProjectName("Old");

		ProjectInformationModel model = new ProjectInformationModel();

		model.setProjectName("New");

		when(repository.findById("P001")).thenReturn(Optional.of(existing));

		service.updateProjectInformation("P001", model);

		verify(repository).save(any(ProjectInformation.class));

		verify(auditService).saveAuditLog(any(), any(), anyString(), anyString(), any(), any(), anyString());
	}

	@Test
	void shouldThrowProjectNotFoundForUpdate() {

		ProjectInformationModel model = new ProjectInformationModel();

		when(repository.findById("P001")).thenReturn(Optional.empty());

		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
				() -> service.updateProjectInformation("P001", model));

		assertEquals(ErrorCode.PROJECT_NOT_FOUND, ex.getErrorCode());
	}

	@Test
	void shouldDeleteProjectInformationSuccessfully() {

		ProjectInformation project = new ProjectInformation();

		project.setId("P001");
		project.setProjectName("Tracker");

		when(repository.findById("P001")).thenReturn(Optional.of(project));

		service.deleteProjectInformation("P001");

		verify(repository).delete(project);

		verify(auditService).saveAuditLog(any(), any(), anyString(), anyString(), any(), any(), anyString());
	}

	@Test
    void shouldThrowProjectNotFoundForDelete() {

        when(repository.findById("P001"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> service.deleteProjectInformation("P001"));

        assertEquals(
                ErrorCode.PROJECT_NOT_FOUND,
                ex.getErrorCode());
    }

	@Test
    void shouldReturnEmptyListWhenNoProjectsExist() {

        when(repository.findAll())
                .thenReturn(List.of());

        service.getAllProjectInformation();

        verify(repository).findAll();
    }

	@Test
	void shouldThrowExceptionWhenRepositoryFailsDuringCreate() {

		ProjectInformationModel model = new ProjectInformationModel();

		model.setProjectName("Tracker");

		ProjectInformation project = new ProjectInformation();

		when(repository.findByProjectName("Tracker")).thenReturn(Optional.empty());

		when(modelMapper.map(any(ProjectInformationModel.class), eq(ProjectInformation.class))).thenReturn(project);

		when(repository.save(any())).thenThrow(new RuntimeException("DB error"));

		assertThrows(RuntimeException.class, () -> service.createProjectInformation(model));
	}

	@Test
	void shouldThrowExceptionWhenUpdateFails() {

		ProjectInformation existing = new ProjectInformation();

		existing.setId("P001");

		ProjectInformationModel model = new ProjectInformationModel();

		when(repository.findById("P001")).thenReturn(Optional.of(existing));

		when(repository.save(any())).thenThrow(new RuntimeException("Database error"));

		assertThrows(RuntimeException.class, () -> service.updateProjectInformation("P001", model));
	}

	@Test
	void shouldThrowExceptionWhenDeleteFails() {

		ProjectInformation project = new ProjectInformation();

		project.setId("P001");

		when(repository.findById("P001")).thenReturn(Optional.of(project));

		doThrow(new RuntimeException("Delete failed")).when(repository).delete(project);

		assertThrows(RuntimeException.class, () -> service.deleteProjectInformation("P001"));
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

		when(repository.findByProjectName("Tracker")).thenReturn(Optional.empty());

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

		when(repository.findByProjectName("Tracker")).thenReturn(Optional.empty());

		when(modelMapper.map(any(), eq(ProjectInformation.class))).thenThrow(new RuntimeException("Mapping failed"));

		assertThrows(RuntimeException.class, () -> service.createProjectInformation(model));
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

		ProjectInformation project = new ProjectInformation();

		project.setProjectName("Tracker");

		when(repository.findByProjectName("Tracker")).thenReturn(Optional.empty());

		when(modelMapper.map(any(), eq(ProjectInformation.class))).thenReturn(project);

		doThrow(new RuntimeException("Audit failed")).when(auditService).saveAuditLog(any(), any(), anyString(),
				anyString(), any(), any(), anyString());

		assertThrows(RuntimeException.class, () -> service.createProjectInformation(model));
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

		when(repository.findByProjectName("Tracker")).thenReturn(Optional.empty());

		when(modelMapper.map(model, ProjectInformation.class)).thenReturn(project);

		when(repository.save(any(ProjectInformation.class))).thenReturn(project);

		when(projectRepository.findByProjectInformationId("PI001")).thenReturn(Optional.empty());

		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
			Project p = invocation.getArgument(0);
			p.setId("DASH001");
			return p;
		});

		// Mock admin assignment
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
	void shouldSetUpdatedTimeDuringUpdate() {

		ProjectInformation existing = new ProjectInformation();

		existing.setId("P001");

		when(repository.findById("P001")).thenReturn(Optional.of(existing));

		service.updateProjectInformation("P001", new ProjectInformationModel());

		assertEquals(true, existing.getUpdatedAt() != null);
	}
}