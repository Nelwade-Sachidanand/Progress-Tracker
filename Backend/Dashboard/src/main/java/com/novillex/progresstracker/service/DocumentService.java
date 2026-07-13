package com.novillex.progresstracker.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.UploadDocumentRequest;

public interface DocumentService {

    Response uploadDocument(UploadDocumentRequest request, MultipartFile file);

	Resource downloadDocument(String documentId);

	Response getDocuments(UploadDocumentRequest request);
	
	Response getDocumentsByProjectId(String projectId);

//    Response viewDocuments(UploadDocumentRequest request);

}