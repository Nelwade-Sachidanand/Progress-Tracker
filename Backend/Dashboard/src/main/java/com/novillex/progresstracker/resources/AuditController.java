package com.novillex.progresstracker.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.service.ActivityUpdateRequestService;
import com.novillex.progresstracker.service.AuditService;

@RestController
@RequestMapping("/audit")
public class AuditController {

	@Autowired
	private AuditService auditService;
	
	private ActivityUpdateRequestService activityUpdateRequestService;

	@GetMapping("/getAllAudit")
	public Response getAuditLogs() {

		return auditService.getAuditLogs();
	}

	/*
	 * @PostMapping("/rollback/{auditId}") public Response
	 * rollbackActivity(@PathVariable String auditId) {
	 * 
	 * return activityUpdateRequestService.rollbackActivity(auditId); }
	 */
}