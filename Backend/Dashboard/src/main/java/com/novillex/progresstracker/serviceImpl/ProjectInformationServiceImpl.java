package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.ProjectInformation;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.ProjectInformationModel;
import com.novillex.progresstracker.repository.ProjectInformationRepository;
import com.novillex.progresstracker.service.ProjectInformationService;
import com.novillex.progresstracker.util.UserContextUtil;

@Service
public class ProjectInformationServiceImpl implements ProjectInformationService {

	@Autowired
	private ProjectInformationRepository repository;

	@Autowired
	private ApplicationContext context;

	@Override
	public Response createProjectInformation(ProjectInformationModel model) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		repository.findByProjectName(model.getProjectName()).ifPresent(project -> {
			throw new ResourceNotFoundException(ErrorCode.PROJECT_ALREADY_EXISTS, "Project information already exists",
					model.getProjectName());
		});

		ProjectInformation project = new ProjectInformation();

		BeanUtils.copyProperties(model, project);

		project.setStatus("ACTIVE");
		project.setCreatedBy(UserContextUtil.getCurrentUser());
		project.setCreatedAt(LocalDateTime.now());
		project.setUpdatedAt(LocalDateTime.now());

		repository.save(project);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Project information created successfully", project);
	}

	@Override
	public Response getAllProjectInformation() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<ProjectInformation> projects = repository.findAll();

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Project information fetched successfully", projects);
	}

	@Override
	public Response getProjectInformationById(String id) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ProjectInformation project = repository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project information not found", id));

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Project information fetched successfully", project);
	}

	@Override
	public Response updateProjectInformation(String id, ProjectInformationModel model) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ProjectInformation project = repository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project information not found", id));

		BeanUtils.copyProperties(model, project);

		project.setUpdatedAt(LocalDateTime.now());

		repository.save(project);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Project information updated successfully", project);
	}

	@Override
	public Response deleteProjectInformation(String id) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ProjectInformation project = repository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project information not found", id));

		repository.delete(project);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Project information deleted successfully", null);
	}
}