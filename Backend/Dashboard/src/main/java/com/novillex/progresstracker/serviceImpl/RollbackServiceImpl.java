package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.ActivityUpdateRequest;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.HierarchyReference;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.service.RollbackService;
import com.novillex.progresstracker.util.HierarchyReferenceUtil;
import com.novillex.progresstracker.util.UserContextUtil;

@Service
public class RollbackServiceImpl implements RollbackService {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ActivityUpdateRequestRepository requestRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private AuditService auditService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public Response rollbackRequest(String requestId, String password, String reason) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		ActivityUpdateRequest request = requestRepository.findById(requestId).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.REQUEST_NOT_FOUND, "Request not found", requestId));

		if (!"APPROVED".equals(request.getStatus()) && !"REJECTED".equals(request.getStatus())) {

			throw new ValidationException(ErrorCode.INVALID_REQUEST,
					"Only approved or rejected requests can be reverted");
		}

		User currentUser = userRepository.findByUsername(UserContextUtil.getCurrentUser())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found", null));

		if (!passwordEncoder.matches(password, currentUser.getPassword())) {

			throw new ValidationException(ErrorCode.INVALID_PASSWORD, "Invalid password");
		}

		Project project = projectRepository.findById(request.getProjectId())
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
						request.getProjectId()));

		HierarchyReference ref = HierarchyReferenceUtil.findHierarchy(project, request.getActivityId());

		ref.getPhase().setPhaseName(request.getOldPhaseName());

		ref.getMilestone().setMilestoneName(request.getOldMilestoneName());

		ref.getTask().setTaskName(request.getOldTaskName());

		ref.getSubTask().setSubTaskName(request.getOldSubTaskName());

		BeanUtils.copyProperties(request.getOldActivity(), ref.getActivity());

		projectRepository.save(project);

		request.setStatus("ROLLED_BACK");

		request.setRollbackReason(reason);

		request.setRolledBackBy(UserContextUtil.getCurrentUser());

		request.setRolledBackAt(LocalDateTime.now());

		requestRepository.save(request);

		notificationService.createNotification("Activity Rolled Back",
				"Changes for activity '" + request.getOldActivityName() + "' Were Rolled Back by Admin",
				"ACTIVITY_ROLLBACK", request.getId(), "/tasks", request.getRequestedByUserId());

		auditService.saveAuditLog(AuditAction.ROLLBACK_ACTIVITY_UPDATE, AuditEntity.ACTIVITY,
				request.getOldActivityName(), project.getProjectName(), request.getNewActivity(),
				request.getOldActivity(), UserContextUtil.getCurrentUser());

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Request rolled back successfully", request);
	}

}
