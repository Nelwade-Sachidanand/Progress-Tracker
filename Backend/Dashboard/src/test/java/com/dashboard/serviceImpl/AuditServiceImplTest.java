package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.entity.AuditLog;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.repository.AuditLogRepository;
import com.novillex.progresstracker.serviceImpl.AuditServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

	@Mock
	private AuditLogRepository auditLogRepository;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private ApplicationContext context;

	@InjectMocks
	private AuditServiceImpl auditService;

	@Test
	void saveAuditLog_ShouldSaveSuccessfully() throws Exception {

	    when(objectMapper.writeValueAsString(any()))
	            .thenReturn("{\"name\":\"test\"}");

	    auditService.saveAuditLog("CREATE","PROJECT","Demo Project","Demo Project",new Object(),
	            new Object(),"admin");

	    verify(auditLogRepository).save(any(AuditLog.class));
	}

	@Test
	void saveAuditLog_ShouldSaveNullOldAndNewData() {

		auditService.saveAuditLog("CREATE", "PROJECT", "Demo Project", "Demo Project", null, null, "admin");

		verify(auditLogRepository).save(any(AuditLog.class));
	}

	@Test
	void saveAuditLog_ShouldThrowDatabaseException_WhenSerializationFails()
	        throws Exception {

	    when(objectMapper.writeValueAsString(any()))
	            .thenThrow(new RuntimeException("JSON Error"));

	    assertThrows(
	            DatabaseException.class,
	            () -> auditService.saveAuditLog( "CREATE", "PROJECT","Demo Project","Demo Project",new Object(),
	                    new Object(),"admin"));
	}

	@Test
	void saveAuditLog_ShouldThrowDatabaseException_WhenRepositorySaveFails()
	        throws Exception {

	    when(objectMapper.writeValueAsString(any()))
	            .thenReturn("{}");

	    doThrow(new RuntimeException("DB Error"))
	            .when(auditLogRepository)
	            .save(any(AuditLog.class));

	    assertThrows(
	            DatabaseException.class,
	            () -> auditService.saveAuditLog("CREATE","PROJECT",	"Demo Project","Demo Project",new Object(),
	                    new Object(),"admin"));
	}

	@Test
	void getAuditLogs_ShouldReturnLogsSuccessfully() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);
		Response response = new Response();

		AuditLog log = new AuditLog();

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(auditLogRepository.findAll()).thenReturn(List.of(log));

		when(responseBuilder.createResponse(any(), any(), anyString(), any())).thenReturn(response);

		Response result = auditService.getAuditLogs();

		assertNotNull(result);

		verify(auditLogRepository).findAll();
	}

	@Test
	void getAuditLogs_ShouldThrowResourceNotFound_WhenNoLogsFound() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(auditLogRepository.findAll()).thenReturn(new ArrayList<>());

		assertThrows(ResourceNotFoundException.class, () -> auditService.getAuditLogs());

		verify(auditLogRepository).findAll();
	}

	@Test
	void getAuditLogs_ShouldThrowDatabaseException_WhenRepositoryFails() {

		ResponseBuilder responseBuilder = mock(ResponseBuilder.class);

		when(context.getBean(ResponseBuilder.class)).thenReturn(responseBuilder);

		when(auditLogRepository.findAll()).thenThrow(new RuntimeException("DB Error"));

		assertThrows(DatabaseException.class, () -> auditService.getAuditLogs());

		verify(auditLogRepository).findAll();
	}

}
