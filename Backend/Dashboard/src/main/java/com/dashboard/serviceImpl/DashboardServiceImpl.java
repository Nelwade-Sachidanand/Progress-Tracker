package com.dashboard.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import com.dashboard.exception.DatabaseException;
import com.dashboard.exception.ReadExcelException;
import com.dashboard.exception.ResourceNotFoundException;
import com.dashboard.model.ActivityModel;
import com.dashboard.model.AuditLogModel;
import com.dashboard.model.ExcelRowModel;
import com.dashboard.model.GenerateReportModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.AuditService;
import com.dashboard.service.DashboardService;
import com.dashboard.util.ExcelParserUtil;
import com.dashboard.util.UserContextUtil;
import com.dashboard.util.WriteUtil;

@Service
public class DashboardServiceImpl implements DashboardService {

	private static final Logger logger = LoggerFactory.getLogger(UpdateActivityServiceImpl.class);

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private AuditService auditService;

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
				|| !Objects.equals(oldActivity.getScheduleHealth(), newActivity.getScheduleHealth());
	}


	@Override
	public ByteArrayInputStream exportExcel(String projectName) {

		try {
			Optional<Project> optionalProject = projectRepository.findByProjectName(projectName);

			if (optionalProject.isEmpty()) {

				logger.warn("Project not found for export. Project: {}", projectName);

				throw new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", projectName);
			}

			Project project = optionalProject.get();
			ClassPathResource resource = new ClassPathResource("templates/Project_Template.xlsx");
			try (Workbook workbook = WorkbookFactory.create(resource.getInputStream())) {

				Sheet sheet = workbook.getSheet("Project schedule");
				sheet.getRow(1).getCell(3).setCellValue(project.getBankName());
				sheet.getRow(2).getCell(3).setCellValue(project.getProjectName());
				sheet.getRow(3).getCell(3).setCellValue(project.getProjectManager());

				int templateRow = 7;
				int currentRow = templateRow;

				int srNo = 1;
				if (project.getPhases() != null) {
					for (Phase phase : project.getPhases()) {
						if (phase.getMilestones() == null) {
							continue;
						}

						for (Milestone milestone : phase.getMilestones()) {
							if (milestone.getTasks() == null) {
								continue;
							}
							for (Task task : milestone.getTasks()) {
								if (task.getSubTasks() == null) {
									continue;
								}
								for (Subtask subTask : task.getSubTasks()) {
									if (subTask.getActivities() == null) {
										continue;
									}
									for (Activity activity : subTask.getActivities()) {
										Row row;
										if (currentRow == templateRow) {
											row = sheet.getRow(templateRow);
										} else {

											row = copyTemplateRow(sheet, templateRow, currentRow);
										}

										WriteUtil.setCell(row, 0, (srNo++) * 100);
										WriteUtil.setCell(row, 1, phase.getPhaseName());
										WriteUtil.setCell(row, 2, milestone.getMilestoneName());
										WriteUtil.setCell(row, 3, task.getTaskName());
										WriteUtil.setCell(row, 4, subTask.getSubTaskName());
										WriteUtil.setCell(row, 5, activity.getActivityName());
										WriteUtil.setCell(row, 6, "");
										WriteUtil.setCell(row, 7, activity.getEstimatedPeriodWeek());
										WriteUtil.setDate(row, 8, activity.getPlannedStartDate());
										WriteUtil.setDate(row, 9, activity.getPlannedEndDate());
										WriteUtil.setDate(row, 10, activity.getActualStartDate());
										WriteUtil.setDate(row, 11, activity.getActualEndDate());
										WriteUtil.setCell(row, 13, activity.getProgress());

										currentRow++;
									}
								}
							}
						}
					}
				}

				FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
				evaluator.evaluateAll();
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				workbook.write(output);
				String modifiedBy = UserContextUtil.getCurrentUser();

				auditService.saveAuditLog(AuditAction.EXPORT_EXCEL, AuditEntity.PROJECT, project.getProjectName(),
						project.getProjectName(), null, null, modifiedBy);
				logger.info("Excel exported successfully. Project: {}, User: {}", projectName, modifiedBy);
				return new ByteArrayInputStream(output.toByteArray());
			}
		} catch (ResourceNotFoundException e) {
			throw e;

		} catch (Exception e) {
			logger.error("Excel export failed. Project: {}", projectName, e);
			throw new ReadExcelException(ErrorCode.EXCEL_EXPORT_ERROR,
					"Error while exporting excel : " + e.getMessage());
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

	@Override
	public List<ActivityModel> generateReport(GenerateReportModel req) {

		logger.info("Report generation started. Project: {}", req.getProjectName());

		List<ActivityModel> rows = new ArrayList<>();

		try {

			String projectName = req.getProjectName();

			Optional<Project> resultProject = projectRepository.findByProjectName(projectName);

			if (resultProject.isEmpty()) {

				logger.warn("Project not found while generating report. Project: {}", projectName);

				throw new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", projectName);
			}

			Project project = resultProject.get();

			logger.info("Project found successfully. Project: {}", projectName);

			for (Phase phase : project.getPhases()) {

				if (hasText(req.getPhaseName()) && !phase.getPhaseName().equalsIgnoreCase(req.getPhaseName())) {
					continue;
				}

				for (Milestone milestone : phase.getMilestones()) {

					if (hasText(req.getMilestoneName())
							&& !milestone.getMilestoneName().equalsIgnoreCase(req.getMilestoneName())) {
						continue;
					}

					for (Task task : milestone.getTasks()) {

						if (hasText(req.getTaskName()) && !task.getTaskName().equalsIgnoreCase(req.getTaskName())) {
							continue;
						}

						for (Subtask subTask : task.getSubTasks()) {

							if (hasText(req.getSubtaskName())
									&& !subTask.getSubTaskName().equalsIgnoreCase(req.getSubtaskName())) {
								continue;
							}

							for (Activity activity : subTask.getActivities()) {

								if (hasText(req.getExecutionStatus())
										&& !activity.getExecutionStatus().equalsIgnoreCase(req.getExecutionStatus())) {
									continue;
								}

								ActivityModel row = new ActivityModel();

								BeanUtils.copyProperties(activity, row);

								row.setProjectName(project.getProjectName());
								row.setPhaseName(phase.getPhaseName());
								row.setMilestoneName(milestone.getMilestoneName());
								row.setTaskName(task.getTaskName());
								row.setSubTaskName(subTask.getSubTaskName());

								rows.add(row);
							}
						}
					}
				}
			}
			if (rows.isEmpty()) {

				logger.warn("No records found for report. Project: {}, Filters Applied", projectName);

				throw new ResourceNotFoundException(ErrorCode.NO_REPORT_DATA_FOUND,
						"No records found for selected filters", projectName);
			}
			logger.info("Report generated successfully. Project: {}, Records Found: {}", projectName, rows.size());

			return rows;
		} catch (ResourceNotFoundException e) {

			throw e;

		} catch (Exception e) {

			logger.error("Error while generating report. Project: {}", req.getProjectName(), e);

			throw new DatabaseException(ErrorCode.DATABASE_ERROR, "Error while generating report");
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
//	@Override
//	public Object generateReport(GenerateReportModel req) {
//		String projectName = req.getProjectName();
//		Optional<Project> resultProject = projectRepository.findByProjectName(projectName);
//		Project project = resultProject.get();
//
//		if (resultProject.isEmpty()) {
//			logger.warn("Project not found for export. Project: {}", projectName);
//
//			throw new ResourceNotFoundException(ErrorCode.PROJECT_NOT_FOUND, "Project not found", projectName);
//		}
//
//		if (req.getPhaseName() != null && !req.getPhaseName().isEmpty()) {
//			Phase resultPhase = project.getPhases().stream()
//					.filter(phase -> phase.getPhaseName().equalsIgnoreCase(req.getPhaseName())).findFirst()
//					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PHASE_NOT_FOUND, "Phase Not Found",
//							req.getPhaseName()));
//			
//			
//			 if(req.getMilestoneName() != null && !req.getMilestoneName().isEmpty()) {
//				Milestone resultMilestone = resultPhase.getMilestones().stream()
//						.filter(milestone -> milestone.getMilestoneName()
//								.equalsIgnoreCase(req.getMilestoneName())).findFirst()
//								.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MILESTONE_NOT_FOUND, "Mile Stone Not Found", req.getMilestoneName()));
//				
//				if (req.getTaskName() != null && !req.getTaskName().isEmpty()) {
//					Task resultTask = resultMilestone.getTasks().stream()
//							.filter(task -> task.getTaskName().equalsIgnoreCase(req.getTaskName())).findFirst()
//							.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TASK_NOT_FOUND, "Task Not FOund", req.getTaskName()));
//					
//					if (req.getSubtaskName() != null && !req.getSubtaskName().isEmpty()) {
//						Subtask resulSubtask = resultTask.getSubTasks().stream()
//								.filter(subtask -> subtask.getSubTaskName().equalsIgnoreCase(req.getSubtaskName())).findFirst()
//								.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SUBTASK_NOT_FOUND, "Subtask Not Found", req.getSubtaskName()));
//						
//						return resulSubtask;
//					}
//					
//					return resultTask;
//				}
//				return resultMilestone;
//			}
//
//			return resultPhase;
//		}
//		else if (req.getExecutionStatus() != null && !req.getExecutionStatus().isEmpty()) {
//			List<ActivityModel> activityModels = new ArrayList<>();
//
//			for (Phase phase : project.getPhases()) {
//
//			    for (Milestone milestone : phase.getMilestones()) {
//
//			        for (Task task : milestone.getTasks()) {
//
//			            for (Subtask subTask : task.getSubTasks()) {
//
//			                for (Activity activity : subTask.getActivities()) {
//
//			                    if (activity.getExecutionStatus()
//			                            .equalsIgnoreCase(req.getExecutionStatus())) {
//			                    	
//			                    	ActivityModel activitydto = new ActivityModel();
//			                    	BeanUtils.copyProperties(activity, activitydto);
//			                    	activitydto.setProjectName(projectName);
//			                    	activitydto.setPhaseName(phase.getPhaseName());
//			                    	activitydto.setMilestoneName(milestone.getMilestoneName());
//			                    	activitydto.setTaskName(task.getTaskName());
//			                    	activitydto.setSubTaskName(subTask.getSubTaskName());
//			                    	activityModels.add(activitydto);
//			                    }
//			                }
//			            }
//			        }
//			    }
//			}
//
//			return activityModels;
//		}
//		return resultProject;
//	}

}
