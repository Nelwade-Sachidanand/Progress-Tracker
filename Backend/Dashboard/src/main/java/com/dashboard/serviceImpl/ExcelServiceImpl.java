package com.dashboard.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.AuditAction;
import com.dashboard.common.AuditEntity;
import com.dashboard.common.ErrorCode;
import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Activity;
import com.dashboard.entity.Milestone;
import com.dashboard.entity.Phase;
import com.dashboard.entity.Project;
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.entity.User;
import com.dashboard.exception.DatabaseException;
import com.dashboard.exception.ReadExcelException;
import com.dashboard.exception.ResourceNotFoundException;
import com.dashboard.model.ActivityModel;
import com.dashboard.model.AuditLogModel;
import com.dashboard.model.ExcelRowModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.repository.UserRepository;
import com.dashboard.service.AuditService;
import com.dashboard.service.ExcelService;
import com.dashboard.util.ExcelParserUtil;
import com.dashboard.util.UserContextUtil;
import com.dashboard.util.WriteUtil;

@Service
public class ExcelServiceImpl implements ExcelService {

	private static final Logger logger = LoggerFactory.getLogger(ExcelServiceImpl.class);

	@Autowired
	private AuditService auditService;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApplicationContext context;

	@Override
	public Response uploadExcel(MultipartFile file) {

		logger.info("Excel upload started. File: {}", file.getOriginalFilename());

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			List<ExcelRowModel> rows = ExcelParserUtil.parseExcel(file);

			logger.info("Excel parsed successfully. Rows found: {}", rows.size());

			Map<String, Project> projectMap = new HashMap<>();
			List<Project> newlyCreatedProjects = new ArrayList<>();
			List<AuditLogModel> auditLogs = new ArrayList<>();

			for (ExcelRowModel model : rows) {
				WriteUtil.validateExcelRow(model);

				Project project = projectMap.get(model.getProjectName());

				if (project == null) {
					project = projectRepository.findByProjectName(model.getProjectName()).orElse(null);

					if (project == null) {
						project = new Project();
						project.setBankName(model.getBankName());
						project.setProjectManager(model.getProjectManager());
						project.setProjectName(model.getProjectName());
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
					phase.setPhaseName(model.getPhaseName());
					phase.setMilestones(new ArrayList<>());
					project.getPhases().add(phase);
				}

				Milestone milestone = phase.getMilestones().stream()
						.filter(m -> m.getMilestoneName().equals(model.getMilestoneName())).findFirst().orElse(null);

				if (milestone == null) {
					milestone = new Milestone();
					milestone.setMilestoneName(model.getMilestoneName());
					milestone.setTasks(new ArrayList<>());
					phase.getMilestones().add(milestone);
				}

				Task task = milestone.getTasks().stream().filter(t -> t.getTaskName().equals(model.getTaskName()))
						.findFirst().orElse(null);

				if (task == null) {
					task = new Task();
					task.setTaskName(model.getTaskName());
					task.setSubTasks(new ArrayList<>());
					milestone.getTasks().add(task);
				}

				Subtask subTask = task.getSubTasks().stream()
						.filter(st -> st.getSubTaskName().equals(model.getSubTaskName())).findFirst().orElse(null);

				if (subTask == null) {
					subTask = new Subtask();
					subTask.setSubTaskName(model.getSubTaskName());
					subTask.setActivities(new ArrayList<>());
					task.getSubTasks().add(subTask);
				}
				Activity activity = subTask.getActivities().stream()
						.filter(a -> a.getActivityName().equals(model.getActivityName())).findFirst().orElse(null);
				boolean isNewActivity = false;

				Activity oldActivity = null;

				if (activity == null) {
					activity = new Activity();
					activity.setActivityName(model.getActivityName());
					subTask.getActivities().add(activity);
					isNewActivity = true;

				} else {

					oldActivity = new Activity();
					BeanUtils.copyProperties(activity, oldActivity);
				}

				// Update Activity Fields

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

				// Audit for New Activity (Only for Existing Projects)

				if (isNewActivity && !newlyCreatedProjects.contains(project)) {

					auditLogs.add(new AuditLogModel(AuditAction.UPLOAD_CREATE_ACTIVITY, AuditEntity.ACTIVITY,
							activity.getActivityName(), project.getProjectName(), null, activity));
				}

				// Audit for Updated Activity (Only for Existing Projects)

				if (!isNewActivity && !newlyCreatedProjects.contains(project)
						&& isActivityChanged(oldActivity, activity)) {

					auditLogs.add(new AuditLogModel(AuditAction.UPLOAD_UPDATE_ACTIVITY, AuditEntity.ACTIVITY,
							activity.getActivityName(), project.getProjectName(), oldActivity, activity));
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

		List<String> projectNames = newlyCreatedProjects.stream().map(Project::getProjectName).toList();

		List<User> admins = userRepository.findByRole("ADMIN");

		for (User admin : admins) {

			if (admin.getProjectNames() == null) {
				admin.setProjectNames(new ArrayList<>());
			}

			for (String projectName : projectNames) {

				if (!admin.getProjectNames().contains(projectName)) {
					admin.getProjectNames().add(projectName);
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
		ClassPathResource resource = new ClassPathResource("templates/Project_Template.xlsx");

		try (Workbook workbook = WorkbookFactory.create(resource.getInputStream())) {

			Sheet sheet = workbook.getSheet("Project schedule");
			if (sheet == null) {

				throw new ResourceNotFoundException(ErrorCode.EXCEL_TEMPLATE_NOT_FOUND,
						"Project schedule sheet not found in template", "Project schedule");
			}

			Project project = projectRepository.findByProjectName(reports.get(0).getProjectName())
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found",
							reports.get(0).getProjectName()));

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

				currentRow++;
			}

			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateAll();
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
