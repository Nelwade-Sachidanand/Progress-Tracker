package com.dashboard.serviceImpl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Project;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

	private static final Logger logger = LoggerFactory.getLogger(UpdateActivityServiceImpl.class);

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ApplicationContext context;

	@Override
	public Response getAllProjects() {
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		List<Project> projects = projectRepository.findAll();
		logger.info("Projects fetched successfully. Count: {}", projects.size());
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Projects Fetched Successfully", projects);
	}
}