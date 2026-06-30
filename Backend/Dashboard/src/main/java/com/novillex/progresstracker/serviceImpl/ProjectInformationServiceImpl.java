package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.ProjectInformation;
import com.novillex.progresstracker.exception.ApplicationException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.ProjectInformationModel;
import com.novillex.progresstracker.repository.ProjectInformationRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.ProjectInformationService;
import com.novillex.progresstracker.util.UserContextUtil;

@Service
public class ProjectInformationServiceImpl implements ProjectInformationService {

	private static final Logger logger = LoggerFactory.getLogger(ExcelServiceImpl.class);

	@Autowired
	private ProjectInformationRepository repository;

	@Autowired
	private ApplicationContext context;

	@Autowired
	AuditService auditService;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public Response createProjectInformation(ProjectInformationModel model) {

		logger.info("Project information save initiated. ProjectName={}, RequestedBy={}", model.getProjectName(),
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			Optional<ProjectInformation> existingProjectOpt = repository.findByProjectName(model.getProjectName());

			// ==========================
			// UPDATE
			// ==========================
			if (existingProjectOpt.isPresent()) {

				ProjectInformation existingProject = existingProjectOpt.get();

				if (!hasChanges(existingProject, model)) {

					return responseBuilder.createResponse(StatusCode.ERROR, StatusCode.ERROR_STATUS_TYPE,
							"No changes found.", existingProject);
				}

				ProjectInformation oldProject = new ProjectInformation();
				BeanUtils.copyProperties(existingProject, oldProject);

				modelMapper.map(model, existingProject);
				
				existingProject.setId(oldProject.getId());
				existingProject.setCreatedAt(oldProject.getCreatedAt());
				existingProject.setCreatedBy(oldProject.getCreatedBy());
				existingProject.setStatus(oldProject.getStatus());

				existingProject.setUpdatedAt(LocalDateTime.now());
				existingProject.setUpdatedBy(UserContextUtil.getCurrentUser());

				repository.save(existingProject);

				auditService.saveAuditLog(AuditAction.UPDATE_PROJECT_INFORMATION, AuditEntity.PROJECT,
						existingProject.getProjectName(), existingProject.getProjectName(), oldProject, existingProject,
						UserContextUtil.getCurrentUser());

				logger.info("Project information updated successfully. ProjectName={}, UpdatedBy={}",
						existingProject.getProjectName(), UserContextUtil.getCurrentUser());

				return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
						"Project information updated successfully.", existingProject);
			}

			// ==========================
			// CREATE
			// ==========================

			ProjectInformation project = modelMapper.map(model, ProjectInformation.class);

			project.setStatus("ACTIVE");
			project.setCreatedBy(UserContextUtil.getCurrentUser());
			project.setCreatedAt(LocalDateTime.now());
			project.setUpdatedBy(UserContextUtil.getCurrentUser());
			project.setUpdatedAt(LocalDateTime.now());

			repository.save(project);

			auditService.saveAuditLog(AuditAction.CREATE_PROJECT_INFORMATION, AuditEntity.PROJECT,
					project.getProjectName(), project.getProjectName(), null, project,
					UserContextUtil.getCurrentUser());

			logger.info("Project information created successfully. ProjectName={}, CreatedBy={}",
					project.getProjectName(), UserContextUtil.getCurrentUser());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Project information created successfully.", project);

		} catch (Exception ex) {

			logger.error("Error while saving project information. ProjectName={}", model.getProjectName(), ex);

			throw new ApplicationException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to save project information.");
		}
	}

	private boolean hasChanges(ProjectInformation entity, ProjectInformationModel model) {

		return !Objects.equals(entity.getProjectName(), model.getProjectName())
				|| !Objects.equals(entity.getBankName(), model.getBankName())
				|| !Objects.equals(entity.getProjectManager(), model.getProjectManager())
				|| !Objects.equals(entity.getSalesPerson(), model.getSalesPerson())
				|| !Objects.equals(entity.getHeadOfficeAddress(), model.getHeadOfficeAddress())
				|| !Objects.equals(entity.getHeadOfficeContactNo(), model.getHeadOfficeContactNo())
				|| !Objects.equals(entity.getNoOfBranches(), model.getNoOfBranches())
				|| !Objects.equals(entity.getBankType(), model.getBankType())
				|| !Objects.equals(entity.getContactDetails(), model.getContactDetails())
				|| !Objects.equals(entity.getCbsInformation(), model.getCbsInformation())
				|| !Objects.equals(entity.getBusinessStatistics(), model.getBusinessStatistics())
				|| !Objects.equals(entity.getInfrastructure(), model.getInfrastructure())
				|| !Objects.equals(entity.getHardwareDetails(), model.getHardwareDetails())
				|| !Objects.equals(entity.getDigitalChannels(), model.getDigitalChannels())
				|| !Objects.equals(entity.getPaymentSystems(), model.getPaymentSystems());
	}

	@Override
	public Response getAllProjectInformation() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			List<ProjectInformation> projects = repository.findAll();

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Project information fetched successfully", projects);

		} catch (Exception ex) {

			logger.error("Failed to fetch project information records.", ex);

			throw ex;
		}
	}

	@Override
	public Response getProjectInformationById(String id) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			ProjectInformation project = repository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND,
							"Project information not found", id));

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Project information fetched successfully", project);

		} catch (Exception ex) {

			logger.error("Failed to fetch project information. ProjectId={}", id, ex);

			throw ex;
		}
	}

	@Override
	public Response getProjectInformation(String bankName, String projectName) {

		logger.info("Fetching project information. BankName={}, ProjectName={}", bankName, projectName);

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ProjectInformation project = repository.findByProjectNameAndBankName(projectName, bankName).orElseThrow(() -> {

			logger.warn("Project information not found. BankName={}, ProjectName={}", bankName, projectName);

			return new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Project information not found",
					projectName);
		});

		logger.info("Project information fetched successfully. BankName={}, ProjectName={}", bankName, projectName);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Project information fetched successfully", project);
	}

	@Override
	public Response updateProjectInformation(String id, ProjectInformationModel model) {

		logger.info("Project information update initiated. ProjectId={}, UpdatedBy={}", id,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			ProjectInformation project = repository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND,
							"Project information not found", id));

			ProjectInformation oldProject = new ProjectInformation();
			BeanUtils.copyProperties(project, oldProject);

			BeanUtils.copyProperties(model, project);

			project.setUpdatedAt(LocalDateTime.now());

			repository.save(project);

			auditService.saveAuditLog(AuditAction.UPDATE_PROJECT_INFORMATION, AuditEntity.PROJECT,
					project.getProjectName(), project.getProjectName(), oldProject, project,
					UserContextUtil.getCurrentUser());

			logger.info("Project information updated successfully. ProjectId={}, ProjectName={}", project.getId(),
					project.getProjectName());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Project information updated successfully", project);

		} catch (Exception ex) {

			logger.error("Failed to update project information. ProjectId={}", id, ex);

			throw ex;
		}
	}

	@Override
	public Response deleteProjectInformation(String id) {

		logger.info("Project information deletion initiated. ProjectId={}, DeletedBy={}", id,
				UserContextUtil.getCurrentUser());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			ProjectInformation project = repository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND,
							"Project information not found", id));

			auditService.saveAuditLog(AuditAction.DELETE_PROJECT_INFORMATION, AuditEntity.PROJECT,
					project.getProjectName(), project.getProjectName(), project, null,
					UserContextUtil.getCurrentUser());

			repository.delete(project);

			logger.info("Project information deleted successfully. ProjectId={}, ProjectName={}", project.getId(),
					project.getProjectName());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Project information deleted successfully", null);

		} catch (Exception ex) {

			logger.error("Failed to delete project information. ProjectId={}", id, ex);

			throw ex;
		}
	}
}