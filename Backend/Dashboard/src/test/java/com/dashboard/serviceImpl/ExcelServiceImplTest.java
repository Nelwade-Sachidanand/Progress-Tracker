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
import com.novillex.progresstracker.entity.ProjectInformation;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ReadExcelException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.ExcelRowModel;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectInformationRepository;
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
	private ProjectInformationRepository projectInformationRepository;

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

	private void mockProjectInformation(String projectName, String bankName) {

		ProjectInformation info = new ProjectInformation();

		info.setId("PI001");
		info.setProjectName(projectName);
		info.setBankName(bankName);
		info.setProjectManager("Sachin");

		when(projectInformationRepository.findByProjectNameAndBankName(projectName, bankName))
				.thenReturn(Optional.of(info));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void uploadExcel_ShouldCreateNewProjectSuccessfully() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();

		row.setProjectName("Demo Project");
		row.setBankName("HDFC");

		Response response = new Response();

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			mockProjectInformation("Demo Project", "HDFC");

			when(projectRepository.findByProjectInformationId("PI001")).thenReturn(Optional.empty());

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

			Response result = excelService.uploadExcel(file);

			assertNotNull(result);

			verify(projectRepository).saveAll(any());

		}
	}

	@Test
	void uploadExcel_ShouldThrowReadExcelException_WhenExcelInvalid() {

		MultipartFile file = mock(MultipartFile.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenThrow(new RuntimeException("Invalid excel"));

			assertThrows(ReadExcelException.class, () -> excelService.uploadExcel(file));
		}
	}

	@Test
	void uploadExcel_ShouldThrowDatabaseException_WhenSaveFails() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();

		row.setProjectName("Demo Project");
		row.setBankName("HDFC");

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			mockProjectInformation("Demo Project", "HDFC");

			when(projectRepository.findByProjectInformationId("PI001")).thenReturn(Optional.empty());

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			doThrow(new RuntimeException("DB Error")).when(projectRepository).saveAll(any());

			assertThrows(DatabaseException.class, () -> excelService.uploadExcel(file));

		}
	}

	@Test
	void generateExcel_ShouldThrowException_WhenReportsEmpty() {

		assertThrows(ResourceNotFoundException.class, () -> excelService.generateExcel(new ArrayList<>()));
	}

	@Test
	void uploadExcel_ShouldNotCreateDuplicateApprovalRequest() {

		mockLoggedInUser();

		MultipartFile file = mock(MultipartFile.class);

		ExcelRowModel row = new ExcelRowModel();

		row.setProjectName("Demo Project");
		row.setBankName("HDFC");

		Project project = new Project();

		project.setId("P001");
		project.setProjectName("Demo Project");
		project.setPhases(new ArrayList<>());

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		try (MockedStatic<ExcelParserUtil> parserMock = mockStatic(ExcelParserUtil.class)) {

			parserMock.when(() -> ExcelParserUtil.parseExcel(file)).thenReturn(List.of(row));

			mockProjectInformation("Demo Project", "HDFC");

			when(projectRepository.findByProjectInformationId("PI001")).thenReturn(Optional.of(project));

			when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

			when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(new Response());

			excelService.uploadExcel(file);

			verify(requestRepository, never()).save(any());

		}
	}

}
