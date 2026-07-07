package com.novillex.progresstracker.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.UploadDocumentRequest;
import com.novillex.progresstracker.service.DocumentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documents")
public class DocumentController {

	private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

	private final DocumentService documentService;
	
	public DocumentController(DocumentService documentService) {
		this.documentService=documentService;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public Response uploadDocument(@Valid @ModelAttribute UploadDocumentRequest request,
			@RequestPart("file") MultipartFile file) {

		logger.info("Upload request received. Project={}, Activity={}", request.getProjectName(),
				request.getActivityName());

		return documentService.uploadDocument(request, file);
	}

	@GetMapping("/download/{documentId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) {

		logger.info("Download request received. DocumentId={}", documentId);

		Resource resource = documentService.downloadDocument(documentId);

		String fileName = resource.getFilename();

		MediaType mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);

		return ResponseEntity.ok().contentType(mediaType)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").body(resource);
	}

	@PostMapping("/activity")
	public Response getDocumentsByActivity(@RequestBody UploadDocumentRequest request) {

		logger.info("Fetch documents request received. Project={}, Activity={}", request.getProjectName(),
				request.getActivityName());

		return documentService.getDocuments(request);
	}

	@GetMapping("/getAll")
	public Response getAllDocuments() {

		logger.info("Fetch documents request received");

		return documentService.getAllDocuments();
	}

}