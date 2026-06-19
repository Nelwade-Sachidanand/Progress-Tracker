package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.ActivityUpdateRequest;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ReadExcelException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.ExcelRowModel;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.serviceImpl.ExcelServiceImpl;
import com.novillex.progresstracker.util.ExcelParserUtil;

@ExtendWith(MockitoExtension.class)
public class ExcelServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AuditService auditService;

	@Mock
	private ActivityUpdateRequestRepository requestRepository;

	@Mock
	private ApplicationContext context;

	@InjectMocks
	private ExcelServiceImpl excelService;

	private void mockLoggedInUser() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, new ArrayList<>()));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void uploadExcel_ShouldCreateNewProjectSuccessfully() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		Response response = new Response();

		ExcelRowModel row = new ExcelRowModel();

		row.setProjectName("Demo Project");
		row.setBankName("HDFC");
		row.setProjectManager("Sachin");

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.empty());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = excelService.uploadExcel(file);

			assertNotNull(result);

			verify(projectRepository).saveAll(any());

			verify(auditService, atLeastOnce()).saveAuditLog(any(), any(), any(), any(), any(), any(), any());
		}
	}

	@Test
	void uploadExcel_ShouldNotCreateDuplicateApprovalRequest() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();
		row.setProjectName("Demo Project");

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

			excelService.uploadExcel(file);

			verify(requestRepository, never()).save(any());
		}
	}

	@Test
	void uploadExcel_ShouldThrowDatabaseException_WhenSaveAllFails() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();

		row.setProjectName("Demo Project");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.empty());

			doThrow(new RuntimeException("DB Error")).when(projectRepository).saveAll(any());

			assertThrows(DatabaseException.class, () -> excelService.uploadExcel(file));

			verify(projectRepository).saveAll(any());
		}
	}

	@Test
	void uploadExcel_ShouldCreateApprovalRequest_WhenActivityModified() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();

		row.setProjectName("Demo Project");
		row.setPhaseName("Phase1");
		row.setMilestoneName("Milestone1");
		row.setTaskName("Task1");
		row.setSubTaskName("Sub1");
		row.setActivityName("Activity1");

		row.setProgress(80);

		Activity existingActivity = new Activity();
		existingActivity.setActivityName("Activity1");
		existingActivity.setProgress(50);

		Subtask subtask = new Subtask();
		subtask.setSubTaskName("Sub1");
		subtask.setActivities(new ArrayList<>(List.of(existingActivity)));

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

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			when(requestRepository
					.findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(any(),
							any(), any(), any(), any(), any(), eq("PENDING")))
					.thenReturn(Optional.empty());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

			excelService.uploadExcel(file);

			verify(requestRepository).save(any(ActivityUpdateRequest.class));
		}
	}

	@Test
	void uploadExcel_ShouldNotCreateApprovalRequest_WhenNoChangesFound() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();

		row.setProjectName("Demo Project");
		row.setActivityName("Activity1");
		row.setProgress(50);

		Project project = new Project();
		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(project));

			ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

			excelService.uploadExcel(file);

			verify(requestRepository, never()).save(any());
		}
	}

	@Test
	void uploadExcel_ShouldThrowReadExcelException_WhenExcelInvalid() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenThrow(new RuntimeException("Invalid Excel"));

			assertThrows(ReadExcelException.class, () -> excelService.uploadExcel(file));
		}
	}

	@Test
	void uploadExcel_ShouldAssignProjectsToAdmins() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();
		row.setProjectName("Demo Project");
		row.setBankName("HDFC");
		row.setProjectManager("Sachin");

		User admin = new User();
		admin.setRole("ADMIN");
		admin.setProjectIds(new ArrayList<>());

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.empty());

			when(userRepository.findByRole("ADMIN")).thenReturn(List.of(admin));

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

			excelService.uploadExcel(file);

			verify(userRepository).saveAll(anyList());
		}
	}

	@Test
	void uploadExcel_ShouldNotFail_WhenNoAdminsExist() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();
		row.setProjectName("Demo Project");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.empty());

			when(userRepository.findByRole("ADMIN")).thenReturn(new ArrayList<>());

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

			assertDoesNotThrow(() -> excelService.uploadExcel(file));

			verify(userRepository).saveAll(anyList());
		}
	}

	@Test
	void uploadExcel_ShouldAppendDataToExistingProject() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();

		row.setProjectName("Demo Project");
		row.setPhaseName("New Phase");
		row.setMilestoneName("Milestone1");
		row.setTaskName("Task1");
		row.setSubTaskName("SubTask1");
		row.setActivityName("Activity1");

		Project existingProject = new Project();

		existingProject.setId("P001");
		existingProject.setProjectName("Demo Project");
		existingProject.setPhases(new ArrayList<>());

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(projectRepository.findByProjectName("Demo Project")).thenReturn(Optional.of(existingProject));

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

			excelService.uploadExcel(file);

			verify(projectRepository).saveAll(any());

			verify(auditService, never()).saveAuditLog(eq(AuditAction.CREATE_PROJECT), any(), any(), any(), any(),
					any(), any());

			assertFalse(existingProject.getPhases().isEmpty());
		}
	}

	@Test
	void generateExcel_ShouldThrowException_WhenReportsEmpty() {

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> excelService.generateExcel(new ArrayList<>()));

		assertEquals("No report data found", exception.getMessage());
	}

	@Test
	void generateExcel_ProjectNotFound() {

		mockLoggedInUser();

		ActivityModel report = new ActivityModel();

		report.setProjectId("P001");
		report.setProjectName("Demo Project");

		List<ActivityModel> reports = List.of(report);

		when(projectRepository.findById("P001")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> excelService.generateExcel(reports));

		verify(projectRepository).findById("P001");
	}

	@Test
	void generateExcel_Success() {

		mockLoggedInUser();

		ActivityModel report = new ActivityModel();

		report.setProjectId("P001");
		report.setProjectName("Demo Project");
		report.setPhaseName("Phase1");
		report.setMilestoneName("Milestone1");
		report.setTaskName("Task1");
		report.setSubTaskName("SubTask1");
		report.setActivityName("Activity1");

		Project project = new Project();

		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setBankName("HDFC");
		project.setProjectManager("Sachin");

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		byte[] result = excelService.generateExcel(List.of(report));

		assertNotNull(result);

		assertTrue(result.length > 0);

		verify(projectRepository).findById("P001");
	}

	@Test
	void generateExcel_ShouldAuditExport() {

		mockLoggedInUser();

		ActivityModel report = new ActivityModel();

		report.setProjectId("P001");
		report.setProjectName("Demo Project");

		Project project = new Project();

		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setBankName("HDFC");
		project.setProjectManager("Sachin");

		when(projectRepository.findById("P001")).thenReturn(Optional.of(project));

		excelService.generateExcel(List.of(report));

		verify(auditService).saveAuditLog(eq(AuditAction.EXPORT_EXCEL), eq(AuditEntity.PROJECT), anyString(),
				anyString(), isNull(), any(), anyString());
	}

	@Test
	void uploadExcel_ShouldCreateAuditForNewActivity() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		// Arrange Excel row for a completely new activity

		// Mock parser result

		// Mock project lookup

		// Call uploadExcel()

		// Verify audit created

		verify(auditService).saveAuditLog(eq(AuditAction.CREATE_ACTIVITY), eq(AuditEntity.ACTIVITY), anyString(),
				anyString(), isNull(), any(), anyString());
	}

	@Test
	void uploadExcel_ShouldCreateAuditForUpdatedActivity() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		// Existing activity already present

		// Excel row contains changed values

		// uploadExcel()

		verify(auditService).saveAuditLog(eq(AuditAction.UPDATE_ACTIVITY), eq(AuditEntity.ACTIVITY), anyString(),
				anyString(), any(), any(), anyString());
	}

	@Test
	void uploadExcel_ShouldNotCreateAudit_WhenNoChangesDetected() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		// Existing activity

		// Uploaded row exactly same

		excelService.uploadExcel(file);

		verify(auditService, never()).saveAuditLog(eq(AuditAction.UPDATE_ACTIVITY), any(), any(), any(), any(), any(),
				any());
	}

	@Test
	void uploadExcel_ShouldHandleMultipleRows() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		// Mock 2-3 rows

		excelService.uploadExcel(file);

		verify(projectRepository, atLeastOnce()).saveAll(any());
	}
}
