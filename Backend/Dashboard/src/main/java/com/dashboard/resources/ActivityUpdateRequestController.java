package com.dashboard.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.common.Response;
import com.dashboard.service.ActivityUpdateRequestService;

@RestController
@RequestMapping("/activity-request")
public class ActivityUpdateRequestController {

	@Autowired
	private ActivityUpdateRequestService activityUpdateRequestService;

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
}