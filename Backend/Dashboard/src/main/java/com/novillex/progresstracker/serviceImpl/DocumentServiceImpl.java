package com.novillex.progresstracker.serviceImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
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
import com.novillex.progresstracker.service.DocumentService;
import com.novillex.progresstracker.util.UserContextUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

	private static final Logger logger = LoggerFactory.getLogger(DocumentServiceImpl.class);

	private final DocumentRepository documentRepository;

	private final ResponseBuilder responseBuilder;

	@Value("${document.upload.path}")
	private String documentFolder;

	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "image/jpeg", "image/png");

	@Override
	public Response uploadDocument(UploadDocumentRequest request, MultipartFile file) {

		logger.info("Document upload started. Project={}, Activity={}, User={}", request.getProjectName(),
				request.getActivityName(), UserContextUtil.getCurrentUser());

		validateFile(file);

		Documents documents = documentRepository
				.findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
						request.getProjectName(), request.getBankName(), request.getPhaseName(),
						request.getMilestoneName(), request.getTaskName(), request.getSubTaskName(),
						request.getActivityName())
				.orElse(null);

		if (documents == null) {

			logger.info("Creating new document record.");

			documents = new Documents();

			BeanUtils.copyProperties(request, documents);

			documents.setDocuments(new ArrayList<>());
		}

		try {

			logger.info("Configured upload path: {}", documentFolder);
			logger.info("Current working directory: {}", System.getProperty("user.dir"));

			File activityFolder = createActivityFolder(request);

			logger.info("folder created. path={}", activityFolder);
			String storedFileName = generateFileName(activityFolder, file.getOriginalFilename());

			File destination = new File(activityFolder, storedFileName);

			file.transferTo(destination);
			ActivityDocument activityDocument = new ActivityDocument();
			activityDocument.setDocumentId(UUID.randomUUID().toString());
			activityDocument.setFileName(storedFileName);
			activityDocument.setFilePath(destination.getAbsolutePath());
			activityDocument.setUploadedBy(UserContextUtil.getCurrentUser());
			activityDocument.setUploadedDate(LocalDateTime.now());
			documents.getDocuments().add(activityDocument);

			documentRepository.save(documents);

			logger.info("Document uploaded successfully : {}", storedFileName);

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Document uploaded successfully.", activityDocument);

		} catch (IOException ex) {

			logger.error("Failed to upload document {}", file.getOriginalFilename(), ex);

			throw new ResourceNotFoundException(ErrorCode.FILE_UPLOAD_FAILED, "Unable to upload document.",
					file.getOriginalFilename());
		}
	}

	private void validateFile(MultipartFile file) {

		if (file == null || file.isEmpty()) {

			logger.warn("Document upload failed. File is empty.");

			throw new ValidationException(ErrorCode.FILE_NOT_FOUND, "Please select a document.");
		}

		if (file.getSize() > MAX_FILE_SIZE) {

			logger.warn("File size exceeded. File={}, Size={}", file.getOriginalFilename(), file.getSize());

			throw new ValidationException(ErrorCode.FILE_SIZE_EXCEEDED, "Maximum allowed file size is 10 MB.");
		}

		String contentType = file.getContentType();

		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {

			logger.warn("Invalid file type. File={}, ContentType={}", file.getOriginalFilename(), contentType);

			throw new ValidationException(ErrorCode.INVALID_FILE_TYPE,
					"Only PDF, DOCX, XLSX, JPG and PNG files are allowed.");
		}

		logger.info("File validation completed successfully.");
	}

	private File createActivityFolder(UploadDocumentRequest request) {

		String basePath = Paths.get(documentFolder).toAbsolutePath().toString();

		String folderPath = basePath + File.separator + sanitize(request.getBankName()) + File.separator
				+ sanitize(request.getProjectName()) + File.separator + sanitize(request.getPhaseName())
				+ File.separator + sanitize(request.getMilestoneName());
		
		File folder = new File(folderPath);

		if (!folder.exists()) {

			boolean created = folder.mkdirs();

			logger.info("Creating folder : {}", folder.getAbsolutePath());

			if (!created && !folder.exists()) {

				throw new RuntimeException("Unable to create folder : " + folder.getAbsolutePath());
			}
		}

		return folder;
	}

	private String sanitize(String value) {

		if (value == null) {
			return "";
		}

		return value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
	}

	private String generateFileName(File folder, String originalFileName) {

		File file = new File(folder, originalFileName);

		if (!file.exists()) {

			return originalFileName;
		}

		String name = originalFileName;

		String extension = "";

		int dotIndex = originalFileName.lastIndexOf('.');

		if (dotIndex > 0) {

			name = originalFileName.substring(0, dotIndex);

			extension = originalFileName.substring(dotIndex);
		}

		int counter = 1;

		while (true) {

			String newFileName = name + "(" + counter + ")" + extension;

			File newFile = new File(folder, newFileName);

			if (!newFile.exists()) {

				return newFileName;
			}

			counter++;
		}
	}

	@Override
	public Resource downloadDocument(String documentId) {

		logger.info("Download document initiated. DocumentId={}", documentId);

		Documents documents = documentRepository.findByDocumentsDocumentId(documentId).orElseThrow(() -> {

			logger.warn("Document not found. DocumentId={}", documentId);

			return new ResourceNotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document not found.", documentId);
		});

		ActivityDocument activityDocument = documents.getDocuments().stream()
				.filter(doc -> doc.getDocumentId().equals(documentId)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document not found.",
						documentId));

		File file = new File(activityDocument.getFilePath());

		if (!file.exists()) {

			logger.error("Physical file not found. Path={}", activityDocument.getFilePath());

			throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND, "File not found.",
					activityDocument.getFileName());
		}

		logger.info("Document downloaded successfully. File={}", activityDocument.getFileName());

		return new FileSystemResource(file);
	}

	@Override
	public Response getDocuments(UploadDocumentRequest request) {

		logger.info("Fetching documents. Project={}, Activity={}", request.getProjectName(), request.getActivityName());

		Documents documents = documentRepository
				.findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
						request.getProjectName(), request.getBankName(), request.getPhaseName(),
						request.getMilestoneName(), request.getTaskName(), request.getSubTaskName(),
						request.getActivityName())
				.orElseThrow(() -> {

					logger.warn("No documents found. Project={}, Activity={}", request.getProjectName(),
							request.getActivityName());

					return new ResourceNotFoundException(ErrorCode.DOCUMENT_NOT_FOUND,
							"No documents found for the selected activity.", request.getActivityName());
				});

		logger.info("Documents fetched successfully. Total Documents={}", documents.getDocuments().size());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Documents fetched successfully.", documents.getDocuments());
	}

	@Override
	public Response getAllDocuments() {
		List<Documents> result = documentRepository.findAll();
		
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE, "Documents Fetched Successfully", result);
	}
}