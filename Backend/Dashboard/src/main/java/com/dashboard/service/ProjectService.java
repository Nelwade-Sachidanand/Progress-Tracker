package com.dashboard.service;

import com.dashboard.common.Response;

public interface ProjectService {
	
	public Response deleteProject(String projectId);
	
	public Response getAllProjects();


}
