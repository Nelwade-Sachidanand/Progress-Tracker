package com.novillex.progresstracker.serviceImpl;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.ActivityDocument;
import com.novillex.progresstracker.entity.Documents;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.UploadDocumentRequest;
import com.novillex.progresstracker.repository.DocumentRepository;
import com.novillex.progresstracker.service.DocumentService;
import com.novillex.progresstracker.util.UserContextUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

	private final DocumentRepository documentRepository;
	private final ResponseBuilder responseBuilder;

	private static final String DOCUMENT_FOLDER = "documents";
	private static final String PDF_CONTENT_TYPE = "application/pdf";
	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

	@Override
	public Response uploadDocument(UploadDocumentRequest request, MultipartFile file) {

		// File validation
		if (file == null || file.isEmpty()) {
			throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND, "Please select a PDF document.", null);
		}

		// Allow only PDF
		if (!PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())) {
			throw new ResourceNotFoundException(ErrorCode.INVALID_FILE_TYPE, "Only PDF files are allowed.",
					file.getOriginalFilename());
		}

		// Maximum file size (5 MB)
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new ResourceNotFoundException(ErrorCode.FILE_SIZE_EXCEEDED, "Maximum allowed file size is 5 MB.",
					file.getOriginalFilename());
		}

		Documents document = documentRepository
				.findByProjectNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
						request.getProjectName(), request.getPhaseName(), request.getMilestoneName(),
						request.getTaskName(), request.getSubTaskName(), request.getActivityName())
				.orElse(null);

		if (document == null) {

			document = new Documents();

			BeanUtils.copyProperties(request, document);

			document.setDocuments(new ArrayList<>());
		}

		try {

			File folder = new File(DOCUMENT_FOLDER);

			if (!folder.exists()) {
				folder.mkdirs();
			}

			String storedFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

			String filePath = DOCUMENT_FOLDER + File.separator + storedFileName;

			file.transferTo(new File(filePath));

			ActivityDocument activityDocument = new ActivityDocument();

			activityDocument.setDocumentId(UUID.randomUUID().toString());
			activityDocument.setFileName(file.getOriginalFilename());
			activityDocument.setFilePath(filePath);
			activityDocument.setUploadedBy(UserContextUtil.getCurrentUser());
			activityDocument.setUploadedDate(LocalDateTime.now());

			document.getDocuments().add(activityDocument);

			documentRepository.save(document);

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Document uploaded successfully.", document);

		} catch (IOException e) {

			throw new ResourceNotFoundException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to upload document.",
					file.getOriginalFilename());
		}
	}

	@Override
	public Response viewDocuments(UploadDocumentRequest request) {

		Documents document = documentRepository
				.findByProjectNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
						request.getProjectName(), request.getPhaseName(), request.getMilestoneName(),
						request.getTaskName(), request.getSubTaskName(), request.getActivityName())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DOCUMENT_NOT_FOUND,
						"No documents found for the selected activity.", request.getActivityName()));

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Documents fetched successfully.", document.getDocuments());
	}

}