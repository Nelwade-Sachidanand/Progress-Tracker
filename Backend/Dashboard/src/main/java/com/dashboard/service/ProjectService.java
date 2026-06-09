package com.dashboard.service;

import com.dashboard.common.Response;

public interface ProjectService {
	
	public Response deleteProject(String projectName);
	
	public Response getAllProjects();


}
