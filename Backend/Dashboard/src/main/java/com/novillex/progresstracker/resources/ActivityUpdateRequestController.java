package com.novillex.progresstracker.resources;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.BulkAuthRequestModel;
import com.novillex.progresstracker.service.ActivityUpdateRequestService;

@RestController
@RequestMapping("/activity-request")
@PreAuthorize("hasRole('ADMIN')")
public class ActivityUpdateRequestController {

	private ActivityUpdateRequestService activityUpdateRequestService;

	public ActivityUpdateRequestController(ActivityUpdateRequestService activityUpdateRequestService) {
		this.activityUpdateRequestService = activityUpdateRequestService;
	}

	@GetMapping("/getAllRequests")
	public Response getAllReqests() {
		return activityUpdateRequestService.getAllRequests();
	}

	@GetMapping("/activityUpdateRequest/{requestId}")
	public Response getActivityUpdateRequestById(@PathVariable String requestId) {

		return activityUpdateRequestService.getActivityUpdateRequestById(requestId);
	}

	@GetMapping("/pending")
	public Response getPendingRequests() {

		return activityUpdateRequestService.getPendingRequests();
	}

	@PostMapping("/approve/{requestId}")
	public Response approveRequest(@PathVariable String requestId) {

		return activityUpdateRequestService.approveRequest(requestId);
	}

	@PostMapping("/reject/{requestId}")
	public Response rejectRequest(@PathVariable String requestId, @RequestParam String reason) {

		return activityUpdateRequestService.rejectRequest(requestId, reason);
	}

	@PostMapping("/approve-selected")
	public Response approveSelectedRequests(@RequestBody List<String> requestIds) {

		return activityUpdateRequestService.approveSelectedRequests(requestIds);
	}

	@PostMapping("/reject-selected")
	public Response rejectSelectedRequests(@RequestBody BulkAuthRequestModel request) {

		return activityUpdateRequestService.rejectSelectedRequests(request.getRequestIds(), request.getReason());
	}

}