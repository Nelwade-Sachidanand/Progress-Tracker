package com.novillex.progresstracker.service;

import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.UploadDocumentRequest;

public interface DocumentService {

    Response uploadDocument(UploadDocumentRequest request, MultipartFile file);

    Response viewDocuments(UploadDocumentRequest request);

}