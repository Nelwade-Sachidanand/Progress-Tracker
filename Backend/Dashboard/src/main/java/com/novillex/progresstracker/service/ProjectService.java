package com.novillex.progresstracker.service;

import java.util.List;
import java.util.Map;

import com.novillex.progresstracker.common.Response;

public interface ProjectService {
	
	public Response deleteProject(String projectId);
	public Response getProjectNames(List<String> projectIds);
	public Response getAllProjects();


}
