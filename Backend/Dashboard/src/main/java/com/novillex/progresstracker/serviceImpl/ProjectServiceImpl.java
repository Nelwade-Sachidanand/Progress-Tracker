package com.novillex.progresstracker.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.MilestoneWeightageModel;
import com.novillex.progresstracker.model.MilestoneWeightageResponse;
import com.novillex.progresstracker.model.UpdateMilestoneWeightageRequest;
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
	public Response deleteProject(String projectId) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		String modifiedBy = UserContextUtil.getCurrentUser();

		logger.info("Project deletion initiated. Project Id: {}, Requested By: {}", projectId, modifiedBy);

		try {
			Project project = projectRepository.findById(projectId).orElseThrow(() -> {
				logger.warn("Project not found. Project Id: {}", projectId);

				return new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", projectId);
			});

			List<User> users = userRepository.findByProjectIdsContaining(projectId);
			logger.info("Found {} users mapped with project {}", users.size(), project.getProjectName());

			for (User user : users) {

				if (user.getProjectIds() != null) {

					user.getProjectIds().remove(projectId);
				}
			}

			if (!users.isEmpty()) {

				userRepository.saveAll(users);
			}

			projectRepository.delete(project);

			auditService.saveAuditLog(AuditAction.DELETE_PROJECT, AuditEntity.PROJECT, project.getProjectName(),
					project.getProjectName(), project, null, modifiedBy);

			logger.info("Project deleted successfully. Project Name: {}", project.getProjectName());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Project deleted successfully", null);

		} catch (ResourceNotFoundException e) {

			throw e;

		} catch (Exception e) {

			logger.error("Unable to delete project", e);

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

	@Override
	public Response getProjectNames(List<String> projectIds) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		logger.info("Fetching project names. ProjectIds={}", projectIds);

		try {

			List<Project> projects = projectRepository.findByIdIn(projectIds);

			Map<String, String> projectMap = projects.stream()
					.collect(Collectors.toMap(Project::getId, Project::getProjectName));

			logger.info("Project names fetched successfully. RequestedCount={}, FoundCount={}", projectIds.size(),
					projectMap.size());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Project Names Fetched successfully", projectMap);

		} catch (Exception e) {

			logger.error("Failed to fetch project names. ProjectIds={}", projectIds, e);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to fetch project names");
		}
	}

	@Override
	public Response updateMilestoneWeightages(UpdateMilestoneWeightageRequest request) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		logger.info("Updating milestone weightages. ProjectId={}", request.getProjectId());

		try {

			Project project = projectRepository.findById(request.getProjectId()).orElseThrow(() -> {

				logger.warn("Project not found. ProjectId={}", request.getProjectId());

				return new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
						request.getProjectId());
			});

			double totalWeightage = request.getMilestones().stream().mapToDouble(MilestoneWeightageModel::getWeightage)
					.sum();

			logger.info("Total milestone weightage calculated. TotalWeightage={}", totalWeightage);

			if (totalWeightage != 100) {

				logger.warn("Invalid milestone weightage. Expected=100, Actual={}", totalWeightage);

				throw new IllegalArgumentException("Total milestone weightage must be exactly 100");
			}

			for (MilestoneWeightageModel milestoneReq : request.getMilestones()) {

				Phase phase = project.getPhases().stream()
						.filter(p -> p.getPhaseName().equalsIgnoreCase(milestoneReq.getPhaseName())).findFirst()
						.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PHASE_NOT_FOUND, "Phase not found",
								milestoneReq.getPhaseName()));

				Milestone milestone = phase.getMilestones().stream()
						.filter(m -> m.getMilestoneName().equalsIgnoreCase(milestoneReq.getMilestoneName())).findFirst()
						.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MILESTONE_NOT_FOUND,
								"Milestone not found", milestoneReq.getMilestoneName()));

				milestone.setWeightage(milestoneReq.getWeightage());
			}

			projectRepository.save(project);

			logger.info("Milestone weightages updated successfully. ProjectId={}", request.getProjectId());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Milestone weightages updated successfully", project);

		} catch (ResourceNotFoundException e) {

			throw e;

		} catch (Exception e) {

			logger.error("Failed to update milestone weightages. ProjectId={}", request.getProjectId(), e);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to update milestone weightages");
		}
	}

	@Override
	public Response getMilestoneWeightages(String projectId) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		logger.info("Fetching milestone weightages. ProjectId={}", projectId);

		try {

			Project project = projectRepository.findById(projectId).orElseThrow(() -> {

				logger.warn("Project not found. ProjectId={}", projectId);

				return new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", projectId);
			});

			List<MilestoneWeightageResponse> milestoneList = new ArrayList<>();

			for (Phase phase : project.getPhases()) {

				if (phase.getMilestones() == null) {
					continue;
				}

				for (Milestone milestone : phase.getMilestones()) {

					MilestoneWeightageResponse response = new MilestoneWeightageResponse();

					response.setPhaseName(phase.getPhaseName());

					response.setMilestoneName(milestone.getMilestoneName());

					response.setWeightage(milestone.getWeightage() == null ? 0.0 : milestone.getWeightage());

					milestoneList.add(response);
				}
			}

			logger.info("Milestone weightages fetched successfully. ProjectId={}, Count={}", projectId,
					milestoneList.size());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Milestone weightages fetched successfully", milestoneList);

		} catch (ResourceNotFoundException e) {

			throw e;

		} catch (Exception e) {

			logger.error("Failed to fetch milestone weightages. ProjectId={}", projectId, e);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to fetch milestone weightages");
		}
	}

	@Override
	public Response getProjectsByUserId(String userId) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		logger.info("Fetching projects for user. UserId={}", userId);

		try {

			User user = userRepository.findById(userId).orElseThrow(() -> {

				logger.warn("User not found. UserId={}", userId);

				return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found", userId);
			});

			List<String> projectIds = user.getProjectIds();

			logger.info("User found. Username={}, ProjectCount={}", user.getUsername(),
					projectIds == null ? 0 : projectIds.size());

			if (projectIds == null || projectIds.isEmpty()) {

				logger.info("No projects assigned to user. UserId={}", userId);

				return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
						"No projects assigned to user", new ArrayList<>());
			}

			List<Project> projects = projectRepository.findAllById(projectIds);

			logger.info("Projects fetched successfully. UserId={}, ProjectCount={}", userId, projects.size());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Projects fetched successfully", projects);

		} catch (ResourceNotFoundException e) {

			throw e;

		} catch (Exception e) {

			logger.error("Failed to fetch projects for user. UserId={}", userId, e);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to fetch projects");
		}
	}

}
