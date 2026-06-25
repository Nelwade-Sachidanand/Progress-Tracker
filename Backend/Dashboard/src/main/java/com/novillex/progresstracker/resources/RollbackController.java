package com.novillex.progresstracker.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.RollbackRequestModel;
import com.novillex.progresstracker.service.RollbackService;

@RestController
@RequestMapping("/authorization")
public class RollbackController {
	
	@Autowired
	private RollbackService rollbackService;

	@PostMapping("/rollback/{requestId}")
	public Response rollbackRequest(@PathVariable String requestId, @RequestBody RollbackRequestModel request) {

		return rollbackService.rollbackRequest(requestId, request.getPassword(), request.getReason());
	}
}
