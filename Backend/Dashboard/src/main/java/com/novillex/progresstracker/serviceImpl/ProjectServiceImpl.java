package com.novillex.progresstracker.serviceImpl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.UserModel;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.ProjectService;
import com.novillex.progresstracker.util.UserContextUtil;

@Service
public class ProjectServiceImpl implements ProjectService {

	private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);
	@Autowired
	private ApplicationContext context;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditService auditService;

	@Override
	@Transactional
	public Response deleteProject(String projectName) {
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		String modifiedBy = UserContextUtil.getCurrentUser();

		logger.info("Project deletion initiated. Project Name: {}, Requested By: {}", projectName, modifiedBy);
		try {

			Project project = projectRepository.findByProjectName(projectName).orElseThrow(() -> {

				logger.warn("Project deletion failed. Project not found. Project Name: {}, Requested By: {}",
						projectName, modifiedBy);

				return new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found : ", projectName);
			});

			List<User> users = userRepository.findByProjectNamesContaining(projectName);
			logger.info("Found {} users mapped with project {}", users.size(), projectName);

			for (User user : users) {

				if (user.getProjectNames() != null) {
					user.getProjectNames().remove(projectName);
					logger.info("Updated {} users after project deletion", users.size());
				}
			}

			if (!users.isEmpty()) {
				userRepository.saveAll(users);
			}

			projectRepository.delete(project);
			logger.info("Project deleted successfully from database. Project Name: {}", projectName);

			auditService.saveAuditLog(AuditAction.DELETE_PROJECT, AuditEntity.PROJECT, projectName, projectName,
					project, null, modifiedBy);

			logger.info("Audit log created successfully for deleted project {}", projectName);

			logger.info("Project deletion completed successfully. Project Name: {}, Deleted By: {}", projectName,
					modifiedBy);

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Project deleted successfully", null);

		} catch (ResourceNotFoundException e) {

			logger.warn("Project deletion aborted. Reason: {}", e.getMessage());

			throw e;

		} catch (Exception e) {

			logger.error("Unexpected error while deleting project. Project Name: {}, Requested By: {}", projectName,
					modifiedBy, e);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to delete project");
		}
	}

	@Override
	public Response getAllProjects() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		logger.info("Fetching all projects initiated.");

		try {

			List<Project> projects = projectRepository.findAll();

			logger.info("Projects fetched successfully. Total Projects: {}", projects.size());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Projects fetched successfully", projects);

		} catch (Exception e) {

			logger.error("Failed to fetch projects from database", e);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to fetch projects");
		}
	}

}
