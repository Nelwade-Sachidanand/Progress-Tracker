package com.dashboard.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.entity.AuditLog;
import com.dashboard.service.AuditService;

@RestController
@RequestMapping("/audit")
public class AuditController {

	@Autowired
	private AuditService auditService;

	@GetMapping("/getAllAudit")
	public List<AuditLog> getAuditLogs() {
		return auditService.getAuditLogs();
	}
}