package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;

public interface ProjectService {
	
	public Response deleteProject(String projectName);
	
	public Response getAllProjects();


}
