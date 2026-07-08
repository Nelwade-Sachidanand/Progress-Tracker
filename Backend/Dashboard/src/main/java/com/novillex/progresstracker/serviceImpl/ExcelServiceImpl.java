package com.novillex.progresstracker.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.AuditAction;
import com.novillex.progresstracker.common.AuditEntity;
import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.Activity;
import com.novillex.progresstracker.entity.ActivityUpdateRequest;
import com.novillex.progresstracker.entity.Milestone;
import com.novillex.progresstracker.entity.Phase;
import com.novillex.progresstracker.entity.Project;
import com.novillex.progresstracker.entity.ProjectInformation;
import com.novillex.progresstracker.entity.Subtask;
import com.novillex.progresstracker.entity.Task;
import com.novillex.progresstracker.entity.User;
import com.novillex.progresstracker.exception.DatabaseException;
import com.novillex.progresstracker.exception.ReadExcelException;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.AuditLogModel;
import com.novillex.progresstracker.model.ExcelRowModel;
import com.novillex.progresstracker.repository.ActivityUpdateRequestRepository;
import com.novillex.progresstracker.repository.ProjectInformationRepository;
import com.novillex.progresstracker.repository.ProjectRepository;
import com.novillex.progresstracker.repository.UserRepository;
import com.novillex.progresstracker.service.AuditService;
import com.novillex.progresstracker.service.ExcelService;
import com.novillex.progresstracker.util.ExcelParserUtil;
import com.novillex.progresstracker.util.UserContextUtil;
import com.novillex.progresstracker.util.WriteUtil;

@Service
public class ExcelServiceImpl implements ExcelService {

	private static final Logger logger = LoggerFactory.getLogger(ExcelServiceImpl.class);

	private AuditService auditService;

	private ProjectRepository projectRepository;

	private UserRepository userRepository;

	private ApplicationContext context;

	private ActivityUpdateRequestRepository requestRepository;

	private ProjectInformationRepository projectInformationRepository;

	private ActivityUpdateRequestServiceImpl activityUpdateRequestServiceImpl;

	public ExcelServiceImpl(AuditService auditService, ProjectRepository projectRepository,
							UserRepository userRepository, ApplicationContext context,
							ActivityUpdateRequestRepository requestRepository,
							ProjectInformationRepository projectInformationRepository,
							ActivityUpdateRequestServiceImpl activityUpdateRequestServiceImpl) {

		this.auditService = auditService;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.context = context;
		this.requestRepository = requestRepository;
		this.projectInformationRepository = projectInformationRepository;
		this.activityUpdateRequestServiceImpl = activityUpdateRequestServiceImpl;
	}

	@Transactional
	@Override
	public Response uploadExcel(MultipartFile file) {

		logger.info("Excel upload started. File: {}", file.getOriginalFilename());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			List<ExcelRowModel> rows = ExcelParserUtil.parseExcel(file);

			if (rows.isEmpty()) {
				throw new ReadExcelException(ErrorCode.EXCEL_READ_ERROR, "No data found in Excel");
			}

			String projectName = rows.get(0).getProjectName();
			String bankName = rows.get(0).getBankName();

			ProjectInformation projectInfo = projectInformationRepository
					.findByProjectNameAndBankName(projectName, bankName)
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND,
							"Project information not found for project and bank", projectName));

			Project existingProject = projectRepository.findByProjectInformationId(projectInfo.getId()).orElse(null);

			logger.info("Excel parsed successfully. Rows found: {}", rows.size());

			Map<String, Project> projectMap = new HashMap<>();
			List<Project> newlyCreatedProjects = new ArrayList<>();
			List<AuditLogModel> auditLogs = new ArrayList<>();

			for (ExcelRowModel model : rows) {

				Project project = projectMap.get(model.getProjectName());

				if (project == null) {
					project = existingProject;

					if (project == null) {

						project = new Project();
						project.setProjectInformationId(projectInfo.getId());
						project.setProjectName(projectInfo.getProjectName());
						project.setBankName(projectInfo.getBankName());
						project.setProjectManager(projectInfo.getProjectManager());
						project.setPhases(new ArrayList<>());

						newlyCreatedProjects.add(project);

					} else if (project.getPhases() == null) {

						project.setPhases(new ArrayList<>());
					}

					projectMap.put(model.getProjectName(), project);
				}

				Phase phase = project.getPhases().stream().filter(p -> p.getPhaseName().equals(model.getPhaseName()))
						.findFirst().orElse(null);

				if (phase == null) {

					phase = new Phase();
					phase.setPhaseId(UUID.randomUUID().toString());
					phase.setPhaseName(model.getPhaseName());
					phase.setMilestones(new ArrayList<>());
					project.getPhases().add(phase);
				}

				Milestone milestone = phase.getMilestones().stream()
						.filter(m -> m.getMilestoneName().equals(model.getMilestoneName())).findFirst().orElse(null);

				if (milestone == null) {
					milestone = new Milestone();
					milestone.setMilestoneId(UUID.randomUUID().toString());
					milestone.setMilestoneName(model.getMilestoneName());
					milestone.setTasks(new ArrayList<>());
					phase.getMilestones().add(milestone);
				}

				Task task = milestone.getTasks().stream().filter(t -> t.getTaskName().equals(model.getTaskName()))
						.findFirst().orElse(null);

				if (task == null) {
					task = new Task();
					task.setTaskId(UUID.randomUUID().toString());
					task.setTaskName(model.getTaskName());
					task.setSubTasks(new ArrayList<>());
					milestone.getTasks().add(task);
				}

				Subtask subTask = task.getSubTasks().stream()
						.filter(st -> st.getSubTaskName().equals(model.getSubTaskName())).findFirst().orElse(null);

				if (subTask == null) {
					subTask = new Subtask();
					subTask.setSubTaskId(UUID.randomUUID().toString());
					subTask.setSubTaskName(model.getSubTaskName());
					subTask.setActivities(new ArrayList<>());
					task.getSubTasks().add(subTask);
				}
				Activity activity = subTask.getActivities().stream()
						.filter(a -> a.getActivityName().equals(model.getActivityName())).findFirst().orElse(null);

				boolean isNewActivity = false;

				Activity oldActivity = null;
				Activity newActivity = new Activity();
				newActivity.setActivityName(model.getActivityName());
				newActivity.setEstimatedPeriodWeek(model.getEstimatedPeriodWeek());
				newActivity.setPlannedStartDate(model.getPlannedStartDate());
				newActivity.setPlannedEndDate(model.getPlannedEndDate());
				newActivity.setActualStartDate(model.getActualStartDate());
				newActivity.setActualEndDate(model.getActualEndDate());
				newActivity.setActualPeriodWeek(model.getActualPeriodWeek());
				newActivity.setProgress(model.getProgress());
				newActivity.setExecutionStatus(model.getExecutionStatus());
				newActivity.setScheduleHealth(model.getScheduleHealth());
				newActivity.setRemark(model.getRemark());

				if (activity == null) {
					newActivity.setActivityId(UUID.randomUUID().toString());

					activity = new Activity();

					BeanUtils.copyProperties(newActivity, activity);

					subTask.getActivities().add(activity);

					isNewActivity = true;

				} else {

					oldActivity = new Activity();

					BeanUtils.copyProperties(activity, oldActivity);
					if (Boolean.TRUE.equals(activity.getLocked())
							&& !"ADMIN".equalsIgnoreCase(UserContextUtil.getCurrentUserRole())) {

						logger.info("Skipping locked activity {} during Excel upload", activity.getActivityName());

						continue; // Skip this activity
					}
					if (isActivityChanged(oldActivity, newActivity)) {

						ActivityUpdateRequest existingRequest = requestRepository
								.findByActivityIdAndStatus(activity.getActivityId(), "PENDING").orElse(null);

						if (existingRequest == null) {

							ActivityUpdateRequest request = new ActivityUpdateRequest();

							request.setProjectId(project.getId());
							request.setProjectName(project.getProjectName());

							request.setPhaseId(phase.getPhaseId());
							request.setMilestoneId(milestone.getMilestoneId());
							request.setTaskId(task.getTaskId());
							request.setSubTaskId(subTask.getSubTaskId());
							request.setActivityId(activity.getActivityId());

							request.setOldPhaseName(phase.getPhaseName());
							request.setOldMilestoneName(milestone.getMilestoneName());
							request.setOldTaskName(task.getTaskName());
							request.setOldSubTaskName(subTask.getSubTaskName());

							request.setOldOwner(oldActivity.getOwner());
							request.setOldActivityName(oldActivity.getActivityName());

							request.setNewPhaseName(model.getPhaseName());
							request.setNewMilestoneName(model.getMilestoneName());
							request.setNewTaskName(model.getTaskName());
							request.setNewSubTaskName(model.getSubTaskName());

							request.setNewOwner(model.getOwner()); // if Excel contains owner
							request.setNewActivityName(model.getActivityName());

							request.setOldActivity(oldActivity);
							request.setNewActivity(newActivity);

							request.setRequestType("UPDATE");

							request.setRequestSource("EXCEL");

							request.setRequestedBy(UserContextUtil.getCurrentUser());
							request.setRequestedByRole(UserContextUtil.getCurrentUserRole());
							request.setStatus("PENDING");

							request.setChangeReason("Updated through Excel upload");

							request.setRequestedBy(UserContextUtil.getCurrentUser());

							request.setRequestedByUserId(UserContextUtil.getCurrentUserId());

							request.setRequestedAt(LocalDateTime.now());

							requestRepository.save(request);
						} else {

							logger.info("Pending approval request already exists for activity {}",
									activity.getActivityName());
						}
					}
				}

				if (isNewActivity) {

					activity.setEstimatedPeriodWeek(model.getEstimatedPeriodWeek());
					activity.setPlannedStartDate(model.getPlannedStartDate());
					activity.setPlannedEndDate(model.getPlannedEndDate());
					activity.setActualStartDate(model.getActualStartDate());
					activity.setActualEndDate(model.getActualEndDate());
					activity.setActualPeriodWeek(model.getActualPeriodWeek());
					activity.setProgress(model.getProgress());
					activity.setExecutionStatus(model.getExecutionStatus());
					activity.setScheduleHealth(model.getScheduleHealth());
					activity.setRemark(model.getRemark());
				}

				if (isNewActivity && !newlyCreatedProjects.contains(project)) {

					auditLogs.add(new AuditLogModel(AuditAction.UPLOAD_CREATE_ACTIVITY, AuditEntity.ACTIVITY,
							activity.getActivityName(), project.getProjectName(), null, null, activity));
				}

				if (!isNewActivity && !newlyCreatedProjects.contains(project)
						&& isActivityChanged(oldActivity, newActivity)) {

					auditLogs.add(new AuditLogModel(AuditAction.UPLOAD_UPDATE_ACTIVITY, AuditEntity.ACTIVITY,
							activity.getActivityName(), project.getProjectName(), project.getId(), oldActivity,
							newActivity));
				}
			}
			try {

				projectRepository.saveAll(projectMap.values());
				logger.info("Projects saved successfully. Count: {}", projectMap.size());

			} catch (Exception e) {
				logger.error("Failed to save projects from uploaded Excel", e);
				throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Unable to save projects");
			}
			String modifiedBy = UserContextUtil.getCurrentUser();

			for (Project project : newlyCreatedProjects) {
				auditService.saveAuditLog(AuditAction.CREATE_PROJECT, AuditEntity.PROJECT, project.getProjectName(),
						project.getProjectName(), null, project, modifiedBy);
			}
			for (AuditLogModel audit : auditLogs) {
				auditService.saveAuditLog(audit.getActionType(), audit.getEntityType(), audit.getEntityName(),
						audit.getProjectName(), audit.getOldData(), audit.getNewData(), modifiedBy);
			}

			logger.info("Excel upload completed successfully. Rows Processed: {}, New Projects Imported: {}",
					rows.size(), newlyCreatedProjects.size());

			assignProjectsToAdmins(newlyCreatedProjects);

			logger.info("Successfully assigned {} new projects to admin users.", newlyCreatedProjects.size());

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Excel Uploaded Successfully", rows);

		} catch (DatabaseException e) {
			throw e;

		} catch (Exception e) {
			logger.error("Excel upload failed. File: {}", file.getOriginalFilename(), e);

			throw new ReadExcelException(ErrorCode.EXCEL_READ_ERROR,
					"Error while saving Excel into DB : " + e.getMessage());
		}
	}

	private boolean isActivityChanged(Activity oldActivity, Activity newActivity) {

		return !Objects.equals(oldActivity.getEstimatedPeriodWeek(), newActivity.getEstimatedPeriodWeek())
				|| !Objects.equals(oldActivity.getPlannedStartDate(), newActivity.getPlannedStartDate())
				|| !Objects.equals(oldActivity.getPlannedEndDate(), newActivity.getPlannedEndDate())
				|| !Objects.equals(oldActivity.getActualStartDate(), newActivity.getActualStartDate())
				|| !Objects.equals(oldActivity.getActualEndDate(), newActivity.getActualEndDate())
				|| !Objects.equals(oldActivity.getActualPeriodWeek(), newActivity.getActualPeriodWeek())
				|| !Objects.equals(oldActivity.getProgress(), newActivity.getProgress())
				|| !Objects.equals(oldActivity.getExecutionStatus(), newActivity.getExecutionStatus())
				|| !Objects.equals(oldActivity.getScheduleHealth(), newActivity.getScheduleHealth())
				|| !Objects.equals(oldActivity.getRemark(), newActivity.getRemark());
	}

	private void assignProjectsToAdmins(List<Project> newlyCreatedProjects) {

		if (newlyCreatedProjects.isEmpty()) {
			return;
		}

		List<String> projectIds = newlyCreatedProjects.stream().map(Project::getId).toList();

		List<User> admins = userRepository.findByRole("ADMIN");

		for (User admin : admins) {

			if (admin.getProjectIds() == null) {
				admin.setProjectIds(new ArrayList<>());
			}

			for (String projectId : projectIds) {

				if (!admin.getProjectIds().contains(projectId)) {

					admin.getProjectIds().add(projectId);
				}
			}
		}

		userRepository.saveAll(admins);
	}

	@Override
	public byte[] generateExcel(List<ActivityModel> reports) {

		if (reports == null || reports.isEmpty()) {
			logger.warn("Excel export failed. No report data found.");

			throw new ResourceNotFoundException(ErrorCode.NO_REPORT_DATA_FOUND, "No report data found", null);
		}
		ClassPathResource resource = new ClassPathResource("templates/Project_Template 2.xlsx");

		try (Workbook workbook = WorkbookFactory.create(resource.getInputStream())) {

			Sheet sheet = workbook.getSheet("FinWiz_Project_Schedule");
			if (sheet == null) {

				throw new ResourceNotFoundException(ErrorCode.EXCEL_TEMPLATE_NOT_FOUND,
						"Project schedule sheet not found in template", "Project schedule");
			}

			Project project = projectRepository.findById(reports.get(0).getProjectId())
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
							reports.get(0).getProjectId()));
			sheet.getRow(1).getCell(3).setCellValue(project.getBankName());
			sheet.getRow(2).getCell(3).setCellValue(project.getProjectName());
			sheet.getRow(3).getCell(3).setCellValue(project.getProjectManager());

			int templateRow = 7;
			int currentRow = templateRow;

			int srNo = 1;

			for (ActivityModel report : reports) {

				Row row;
				if (currentRow == templateRow) {
					row = sheet.getRow(templateRow);
				} else {

					row = copyTemplateRow(sheet, templateRow, currentRow);
				}

				WriteUtil.setCell(row, 0, (srNo++) * 100);
				WriteUtil.setCell(row, 1, report.getPhaseName());
				WriteUtil.setCell(row, 2, report.getMilestoneName());
				WriteUtil.setCell(row, 3, report.getTaskName());
				WriteUtil.setCell(row, 4, report.getSubTaskName());
				WriteUtil.setCell(row, 5, report.getActivityName());
				WriteUtil.setCell(row, 6, "");
				WriteUtil.setCell(row, 7, report.getEstimatedPeriodWeek());
				WriteUtil.setDate(row, 8, report.getPlannedStartDate());
				WriteUtil.setDate(row, 9, report.getPlannedEndDate());
				WriteUtil.setDate(row, 10, report.getActualStartDate());
				WriteUtil.setDate(row, 11, report.getActualEndDate());
				WriteUtil.setCell(row, 13, report.getProgress());
				WriteUtil.setCell(row, 16, report.getRemark());

				CellStyle style = workbook.createCellStyle();
				Cell cell = row.getCell(16);
				style.cloneStyleFrom(cell.getCellStyle());
				style.setWrapText(true);
				cell.setCellStyle(style);
				row.setHeight((short) -1);

//				row.setHeight((short) -1); // Auto height (if supported)

				currentRow++;
			}

			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateAll();
			workbook.setForceFormulaRecalculation(true);

			ByteArrayOutputStream output = new ByteArrayOutputStream();

			workbook.write(output);
			logger.info("Excel report generated successfully. Project: {}, Records: {}",
					reports.get(0).getProjectName(), reports.size());
			String modifiedBy = UserContextUtil.getCurrentUser();

			auditService.saveAuditLog(AuditAction.EXPORT_EXCEL, AuditEntity.PROJECT, reports.get(0).getProjectName(),
					reports.get(0).getProjectName(), null, "Project Excel Report Exported", modifiedBy);
			return output.toByteArray();

		} catch (ResourceNotFoundException e) {

			throw e;

		} catch (Exception e) {

			logger.error("Error while generating excel report", e);

			throw new ReadExcelException(ErrorCode.EXCEL_EXPORT_ERROR, "Error while generating Excel report");
		}
	}

	private Row copyTemplateRow(Sheet sheet, int templateRowNum, int newRowNum) {
		Row templateRow = sheet.getRow(templateRowNum);
		Row newRow = sheet.createRow(newRowNum);
		for (int i = 0; i < templateRow.getLastCellNum(); i++) {
			Cell oldCell = templateRow.getCell(i);
			if (oldCell == null)
				continue;

			Cell newCell = newRow.createCell(i);
			newCell.setCellStyle(oldCell.getCellStyle());
			if (oldCell.getCellType() == CellType.FORMULA) {
				String formula = oldCell.getCellFormula();
				formula = formula.replace(String.valueOf(templateRowNum + 1), String.valueOf(newRowNum + 1));
				newCell.setCellFormula(formula);
			}
		}
		return newRow;
	}
}
