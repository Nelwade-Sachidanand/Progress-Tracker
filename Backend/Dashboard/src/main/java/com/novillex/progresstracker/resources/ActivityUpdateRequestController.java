package com.novillex.progresstracker.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.service.ActivityUpdateRequestService;


@RestController
@RequestMapping("/activity-request")
public class ActivityUpdateRequestController {

	@Autowired
	private ActivityUpdateRequestService activityUpdateRequestService;
	
	@GetMapping("/getAllRequests")
	public Response getAllReqests() {
		return activityUpdateRequestService.getAllRequests();
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
	
	
	@PostMapping("/approve-all")
	public Response approveAllRequests() {
	    return activityUpdateRequestService.approveAllRequests();
	}

	@PostMapping("/reject-all/{reson}")
	public Response rejectAllRequests(@PathVariable String reason) {
	    return activityUpdateRequestService.rejectAllRequests(reason);
	}
}