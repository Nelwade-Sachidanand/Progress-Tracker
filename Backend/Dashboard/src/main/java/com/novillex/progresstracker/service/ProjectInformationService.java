package com.novillex.progresstracker.service;

import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.model.ProjectInformationModel;

public interface ProjectInformationService {

	Response createProjectInformation(ProjectInformationModel model);

	Response getAllProjectInformation();

	Response getProjectInformationById(String id);

	Response updateProjectInformation(String id, ProjectInformationModel model);

	Response deleteProjectInformation(String id);
	
	Response getProjectInformation(String bankName, String projectName);
	}