package com.novillex.progresstracker.service;

import java.util.List;
import java.util.Map;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.UpdateMilestoneWeightageRequest;

public interface ProjectService {

	public Response deleteProject(String projectId);

	public Response getProjectNames(List<String> projectIds);

	public Response getAllProjects();

	public Response updateMilestoneWeightages(UpdateMilestoneWeightageRequest request);
	
	public Response getMilestoneWeightages(String projectId);

}
