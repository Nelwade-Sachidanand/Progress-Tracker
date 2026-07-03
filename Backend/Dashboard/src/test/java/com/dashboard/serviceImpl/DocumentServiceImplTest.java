package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.ActivityDocument;
import com.novillex.progresstracker.entity.Documents;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.UploadDocumentRequest;
import com.novillex.progresstracker.repository.DocumentRepository;
import com.novillex.progresstracker.service.VirusScanService;
import com.novillex.progresstracker.serviceImpl.DocumentServiceImpl;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ResponseBuilder responseBuilder;

    @Mock
    private VirusScanService virusScanService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {

        Field field = DocumentServiceImpl.class.getDeclaredField("documentFolder");
        field.setAccessible(true);
        field.set(documentService, tempDir.toString());
    }

    private UploadDocumentRequest buildRequest() {

        UploadDocumentRequest request = new UploadDocumentRequest();

        request.setProjectId("P001");
        request.setProjectName("Demo Project");
        request.setBankName("HDFC");
        request.setPhaseName("Phase1");
        request.setMilestoneName("Milestone1");
        request.setTaskName("Task1");
        request.setSubTaskName("SubTask1");
        request.setActivityName("Activity1");

        return request;
    }

    private Documents buildDocuments() {

        Documents documents = new Documents();

        documents.setProjectId("P001");
        documents.setProjectName("Demo Project");
        documents.setBankName("HDFC");
        documents.setPhaseName("Phase1");
        documents.setMilestoneName("Milestone1");
        documents.setTaskName("Task1");
        documents.setSubTaskName("SubTask1");
        documents.setActivityName("Activity1");
        documents.setDocuments(new ArrayList<>());

        return documents;
    }

    private MockMultipartFile buildPdfFile() {

        return new MockMultipartFile(
                "file",
                "Document.pdf",
                "application/pdf",
                "Sample PDF".getBytes());
    }

    private Response buildResponse() {

        Response response = new Response();

        response.setStatusCode(StatusCode.SUCCESS);
        response.setStatusType(StatusCode.SUCCESS_STATUS_TYPE);
        response.setStatusDesc("Success");
        response.setDetails(null);

        return response;
    }

    private void mockLoggedInUser() {

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));
    }
    
    @Test
    void uploadDocument_Success_NewDocument() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        Documents documents = buildDocuments();

        Response response = buildResponse();

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(documentRepository.save(any(Documents.class))).thenReturn(documents);

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(response);

        Response result = documentService.uploadDocument(request, file);

        assertNotNull(result);

        verify(virusScanService).scan(file);
        verify(documentRepository).save(any(Documents.class));
    }

    @Test
    void uploadDocument_Success_ExistingDocument() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        Documents documents = buildDocuments();

        Response response = buildResponse();

        documents.getDocuments().add(new ActivityDocument());

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(documents));

        when(documentRepository.save(any(Documents.class))).thenReturn(documents);

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(response);

        Response result = documentService.uploadDocument(request, file);

        assertNotNull(result);

        verify(virusScanService).scan(file);
        verify(documentRepository).save(any(Documents.class));
    }

    @Test
    void uploadDocument_FileEmpty() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                new byte[0]);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> documentService.uploadDocument(request, file));

        assertEquals(ErrorCode.FILE_NOT_FOUND, exception.getErrorCode());

        verify(documentRepository, never()).save(any());
        verify(responseBuilder, never()).createResponse(any(), any(), anyString(), any());

        verify(virusScanService, never()).scan(any());
    }

    @Test
    void uploadDocument_FileSizeExceeded() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        byte[] largeFile = new byte[11 * 1024 * 1024];

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeFile);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> documentService.uploadDocument(request, file));

        assertEquals(ErrorCode.FILE_SIZE_EXCEEDED, exception.getErrorCode());

        verify(documentRepository, never()).save(any());
        verify(responseBuilder, never()).createResponse(any(), any(), anyString(), any());

        verify(virusScanService, never()).scan(any());
    }

    @Test
    void uploadDocument_InvalidContentType() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Invalid File".getBytes());

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> documentService.uploadDocument(request, file));

        assertEquals(ErrorCode.INVALID_FILE_TYPE, exception.getErrorCode());

        verify(documentRepository, never()).save(any());
        verify(responseBuilder, never()).createResponse(any(), any(), anyString(), any());

        verify(virusScanService, never()).scan(any());
    }

    @Test
    void uploadDocument_NullFile() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> documentService.uploadDocument(request, null));

        assertEquals(ErrorCode.FILE_NOT_FOUND, exception.getErrorCode());

        verify(documentRepository, never()).save(any());
        verify(responseBuilder, never()).createResponse(any(), any(), anyString(), any());

        verify(virusScanService, never()).scan(any());
    }

    @Test
    void uploadDocument_IOExceptionWhileTransfer() throws Exception {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getOriginalFilename()).thenReturn("Document.pdf");

        doThrow(new IOException("Disk Error"))
                .when(file)
                .transferTo(any(File.class));

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.uploadDocument(request, file));

        assertEquals(ErrorCode.FILE_UPLOAD_FAILED, exception.getErrorCode());

        verify(virusScanService).scan(file);
        verify(documentRepository, never()).save(any());
        verify(responseBuilder, never()).createResponse(any(), any(), anyString(), any());
    }
    
    @Test
    void uploadDocument_FileAlreadyExists_ShouldRename() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        Documents documents = buildDocuments();

        Response response = buildResponse();

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(documents));

        when(documentRepository.save(any(Documents.class))).thenReturn(documents);

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(response);

        // First upload
        documentService.uploadDocument(request, file);

        // Second upload with same filename
        documentService.uploadDocument(request, file);

        ArgumentCaptor<Documents> captor = ArgumentCaptor.forClass(Documents.class);

        verify(documentRepository, atLeast(2)).save(captor.capture());

        Documents saved = captor.getValue();

        assertEquals(2, saved.getDocuments().size());
        assertEquals("Document.pdf", saved.getDocuments().get(0).getFileName());
        assertEquals("Document(1).pdf", saved.getDocuments().get(1).getFileName());

        verify(virusScanService, times(2)).scan(any(MultipartFile.class));
    }

    @Test
    void downloadDocument_Success() throws Exception {

        Documents documents = buildDocuments();

        File file = new File(tempDir.toFile(), "Document.pdf");
        assertTrue(file.createNewFile());

        ActivityDocument activityDocument = new ActivityDocument();
        activityDocument.setDocumentId("DOC001");
        activityDocument.setFileName("Document.pdf");
        activityDocument.setFilePath(file.getAbsolutePath());
        activityDocument.setUploadedBy("admin");
        activityDocument.setUploadedDate(LocalDateTime.now());

        documents.getDocuments().add(activityDocument);

        when(documentRepository.findByDocumentsDocumentId("DOC001"))
                .thenReturn(Optional.of(documents));

        Resource resource = documentService.downloadDocument("DOC001");

        assertNotNull(resource);
        assertTrue(resource instanceof FileSystemResource);
        assertTrue(resource.exists());

        verify(documentRepository).findByDocumentsDocumentId("DOC001");
    }

    @Test
    void downloadDocument_DocumentNotFound() {

        when(documentRepository.findByDocumentsDocumentId("DOC001"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.downloadDocument("DOC001"));

        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());

        verify(documentRepository).findByDocumentsDocumentId("DOC001");
    }

    @Test
    void downloadDocument_PhysicalFileNotFound() {

        Documents documents = buildDocuments();

        ActivityDocument activityDocument = new ActivityDocument();
        activityDocument.setDocumentId("DOC001");
        activityDocument.setFileName("Document.pdf");
        activityDocument.setFilePath(tempDir.resolve("NotExist.pdf").toString());

        documents.getDocuments().add(activityDocument);

        when(documentRepository.findByDocumentsDocumentId("DOC001"))
                .thenReturn(Optional.of(documents));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.downloadDocument("DOC001"));

        assertEquals(ErrorCode.FILE_NOT_FOUND, exception.getErrorCode());

        verify(documentRepository).findByDocumentsDocumentId("DOC001");
    }

    @Test
    void downloadDocument_DocumentExistsButActivityDocumentNotFound() {

        Documents documents = buildDocuments();

        ActivityDocument activityDocument = new ActivityDocument();
        activityDocument.setDocumentId("DOC002");

        documents.getDocuments().add(activityDocument);

        when(documentRepository.findByDocumentsDocumentId("DOC001"))
                .thenReturn(Optional.of(documents));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.downloadDocument("DOC001"));

        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());

        verify(documentRepository).findByDocumentsDocumentId("DOC001");
    }

    @Test
    void downloadDocument_ShouldReturnFileSystemResource() throws Exception {

        Documents documents = buildDocuments();

        File file = new File(tempDir.toFile(), "Document.pdf");

        assertTrue(file.createNewFile());

        ActivityDocument activity = new ActivityDocument();
        activity.setDocumentId("DOC001");
        activity.setFileName("Document.pdf");
        activity.setFilePath(file.getAbsolutePath());

        documents.getDocuments().add(activity);

        when(documentRepository.findByDocumentsDocumentId("DOC001"))
                .thenReturn(Optional.of(documents));

        Resource resource = documentService.downloadDocument("DOC001");

        assertTrue(resource instanceof FileSystemResource);

        verify(documentRepository).findByDocumentsDocumentId("DOC001");
    }
    
    @Test
    void getDocuments_Success() {

        UploadDocumentRequest request = buildRequest();

        Documents documents = buildDocuments();

        ActivityDocument document = new ActivityDocument();
        document.setDocumentId("DOC001");
        document.setFileName("Document.pdf");
        document.setUploadedBy("admin");
        document.setUploadedDate(LocalDateTime.now());

        documents.getDocuments().add(document);

        Response response = buildResponse();

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(documents));

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(response);

        Response result = documentService.getDocuments(request);

        assertNotNull(result);

        verify(documentRepository)
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString());

        verify(responseBuilder)
                .createResponse(any(), any(), anyString(), any());
    }

    @Test
    void getDocuments_DocumentNotFound() {

        UploadDocumentRequest request = buildRequest();

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.getDocuments(request));

        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());

        verify(documentRepository)
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString());

        verify(responseBuilder, never())
                .createResponse(any(), any(), anyString(), any());
    }

    @Test
    void getDocuments_EmptyDocumentList() {

        UploadDocumentRequest request = buildRequest();

        Documents documents = buildDocuments();
        documents.setDocuments(new ArrayList<>());

        Response response = buildResponse();

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(documents));

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(response);

        Response result = documentService.getDocuments(request);

        assertNotNull(result);

        verify(documentRepository)
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString());

        verify(responseBuilder)
                .createResponse(any(), any(), anyString(), any());
    }

    @Test
    void getDocuments_ShouldReturnDocumentList() {

        UploadDocumentRequest request = buildRequest();

        Documents documents = buildDocuments();

        ActivityDocument activity = new ActivityDocument();
        activity.setDocumentId("DOC001");

        documents.getDocuments().add(activity);

        Response response = buildResponse();

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(documents));

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(response);

        Response result = documentService.getDocuments(request);

        assertNotNull(result);

        verify(responseBuilder).createResponse(
                eq(StatusCode.SUCCESS),
                eq(StatusCode.SUCCESS_STATUS_TYPE),
                eq("Documents fetched successfully."),
                eq(documents.getDocuments()));
    }

    @Test
    void getDocuments_ShouldReturnEmptyDocumentCollection() {

        UploadDocumentRequest request = buildRequest();

        Documents documents = buildDocuments();
        documents.setDocuments(new ArrayList<>());

        Response response = buildResponse();

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(documents));

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(response);

        Response result = documentService.getDocuments(request);

        assertNotNull(result);

        verify(responseBuilder).createResponse(
                eq(StatusCode.SUCCESS),
                eq(StatusCode.SUCCESS_STATUS_TYPE),
                eq("Documents fetched successfully."),
                eq(documents.getDocuments()));
    }
    
    @Test
    void uploadDocument_NullContentType() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn(null);
        when(file.getOriginalFilename()).thenReturn("Document.pdf");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> documentService.uploadDocument(request, file));

        assertEquals(ErrorCode.INVALID_FILE_TYPE, exception.getErrorCode());

        verify(documentRepository, never()).save(any());
        verify(virusScanService, never()).scan(any());
    }

    @Test
    void uploadDocument_ShouldPopulateActivityDocumentCorrectly() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        ArgumentCaptor<Documents> captor = ArgumentCaptor.forClass(Documents.class);

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(documentRepository.save(any(Documents.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(buildResponse());

        documentService.uploadDocument(request, file);

        verify(virusScanService).scan(file);
        verify(documentRepository).save(captor.capture());

        Documents saved = captor.getValue();

        assertEquals(1, saved.getDocuments().size());

        ActivityDocument activity = saved.getDocuments().get(0);

        assertNotNull(activity.getDocumentId());
        assertEquals("Document.pdf", activity.getFileName());
        assertNotNull(activity.getFilePath());
        assertEquals("admin", activity.getUploadedBy());
        assertNotNull(activity.getUploadedDate());
    }

    @Test
    void uploadDocument_ShouldCreateNewDocumentsEntity() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        ArgumentCaptor<Documents> captor = ArgumentCaptor.forClass(Documents.class);

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(documentRepository.save(any(Documents.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(buildResponse());

        documentService.uploadDocument(request, file);

        verify(virusScanService).scan(file);
        verify(documentRepository).save(captor.capture());

        Documents saved = captor.getValue();

        assertEquals(request.getProjectId(), saved.getProjectId());
        assertEquals(request.getProjectName(), saved.getProjectName());
        assertEquals(request.getBankName(), saved.getBankName());
        assertEquals(request.getPhaseName(), saved.getPhaseName());
        assertEquals(request.getMilestoneName(), saved.getMilestoneName());
        assertEquals(request.getTaskName(), saved.getTaskName());
        assertEquals(request.getSubTaskName(), saved.getSubTaskName());
        assertEquals(request.getActivityName(), saved.getActivityName());
    }

    @Test
    void uploadDocument_ShouldAppendToExistingDocuments() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        Documents documents = buildDocuments();

        documents.getDocuments().add(new ActivityDocument());

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(documents));

        when(documentRepository.save(any(Documents.class))).thenReturn(documents);

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(buildResponse());

        documentService.uploadDocument(request, file);

        assertEquals(2, documents.getDocuments().size());

        verify(virusScanService).scan(file);
        verify(documentRepository).save(documents);
    }

    @Test
    void uploadDocument_ShouldReturnResponseFromResponseBuilder() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        Documents documents = buildDocuments();

        Response expected = buildResponse();

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(documentRepository.save(any(Documents.class))).thenReturn(documents);

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(expected);

        Response actual = documentService.uploadDocument(request, file);

        verify(virusScanService).scan(file);

        assertSame(expected, actual);
    }

    @Test
    void uploadDocument_ShouldGenerateUniqueDocumentId() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        ArgumentCaptor<Documents> captor = ArgumentCaptor.forClass(Documents.class);

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(documentRepository.save(any(Documents.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(buildResponse());

        documentService.uploadDocument(request, file);

        verify(virusScanService).scan(file);
        verify(documentRepository).save(captor.capture());

        String documentId = captor.getValue()
                .getDocuments()
                .get(0)
                .getDocumentId();

        assertNotNull(documentId);
        assertFalse(documentId.isBlank());
    }

    @Test
    void uploadDocument_ShouldStoreAbsoluteFilePath() {

        mockLoggedInUser();

        UploadDocumentRequest request = buildRequest();

        MockMultipartFile file = buildPdfFile();

        ArgumentCaptor<Documents> captor = ArgumentCaptor.forClass(Documents.class);

        when(documentRepository
                .findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
                        anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(documentRepository.save(any(Documents.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(responseBuilder.createResponse(any(), any(), anyString(), any()))
                .thenReturn(buildResponse());

        documentService.uploadDocument(request, file);

        verify(virusScanService).scan(file);
        verify(documentRepository).save(captor.capture());

        String path = captor.getValue()
                .getDocuments()
                .get(0)
                .getFilePath();

        assertNotNull(path);
        assertTrue(new File(path).isAbsolute());
    }
}