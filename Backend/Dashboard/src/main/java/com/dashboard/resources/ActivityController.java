package com.dashboard.resources;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dashboard.common.Response;
import com.dashboard.model.Activity;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin
public class ActivityController {
	
	@PutMapping("/update/activity")
	public Response updateActivity(@RequestBody Activity updateActivity, HttpServletRequest request) {
		System.out.println(request.getHeader("Authorization"));
		System.out.println(updateActivity);
		return null;
	}
}
