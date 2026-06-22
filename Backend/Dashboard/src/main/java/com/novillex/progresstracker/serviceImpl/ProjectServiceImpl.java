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

		List<Project> projects = projectRepository.findByIdIn(projectIds);

		Map<String, String> projectMap = projects.stream()
				.collect(Collectors.toMap(Project::getId, Project::getProjectName));
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Project Names Fetched successfully", projectMap);

	}

	@Override
	public Response updateMilestoneWeightages(UpdateMilestoneWeightageRequest request) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		Project project = projectRepository.findById(request.getProjectId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
						request.getProjectId()));

		double totalWeightage = request.getMilestones().stream().mapToDouble(MilestoneWeightageModel::getWeightage)
				.sum();

		if (totalWeightage != 100) {

			throw new IllegalArgumentException("Total milestone weightage must be exactly 100");
		}

		Phase phase = project.getPhases().stream()
				.filter(p -> p.getPhaseName().equalsIgnoreCase(request.getPhaseName())).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PHASE_NOT_FOUND, "Phase not found",
						request.getPhaseName()));

		for (MilestoneWeightageModel milestoneReq : request.getMilestones()) {

			Milestone milestone = phase.getMilestones().stream()
					.filter(m -> m.getMilestoneName().equalsIgnoreCase(milestoneReq.getMilestoneName())).findFirst()
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MILESTONE_NOT_FOUND,
							"Milestone not found", milestoneReq.getMilestoneName()));

			milestone.setWeightage(milestoneReq.getWeightage());
		}

		projectRepository.save(project);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Milestone weightages updated successfully", project);
	}

	@Override
	public Response getMilestoneWeightages(String projectId) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		Project project = projectRepository.findById(projectId).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", projectId));

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

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Milestone weightages fetched successfully", milestoneList);
	}

}
