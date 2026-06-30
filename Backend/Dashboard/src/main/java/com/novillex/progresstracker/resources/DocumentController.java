package com.novillex.progresstracker.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.UploadDocumentRequest;
import com.novillex.progresstracker.service.DocumentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/documets")
@PreAuthorize("hasRole('ADMIN')")
public class DocumentController {
	
	@Autowired
	private DocumentService documentService;

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Response uploadDocument(@Valid @ModelAttribute UploadDocumentRequest request,
			@RequestPart("file") MultipartFile file) {

		return documentService.uploadDocument(request, file);
	}
}
