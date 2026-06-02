package com.dashboard.resources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.common.Response;
import com.dashboard.model.ActivityModel;
import com.dashboard.service.UpdateActivityService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin
public class ActivityController {
	
	@Autowired
	private UpdateActivityService updateActivityService;
	
	@PutMapping("/update/activity")
	public Response updateActivity(@RequestBody ActivityModel updateActivity) {
//		System.out.println(request.getHeader("Authorization"));
//		System.out.println(updateActivity);
		return updateActivityService.updateActivity(updateActivity);
	}
}
