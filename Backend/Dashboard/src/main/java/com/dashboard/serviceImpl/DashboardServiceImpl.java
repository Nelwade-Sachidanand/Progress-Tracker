package com.dashboard.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dashboard.common.Response;
import com.dashboard.common.ResponseBuilder;
import com.dashboard.common.StatusCode;
import com.dashboard.entity.Activity;
import com.dashboard.entity.Milestone;
import com.dashboard.entity.Phase;
import com.dashboard.entity.Project;
import com.dashboard.entity.Subtask;
import com.dashboard.entity.Task;
import com.dashboard.exception.ReadExcelException;
import com.dashboard.model.ExcelRowModel;
import com.dashboard.repository.ProjectRepository;
import com.dashboard.service.DashboardService;
import com.dashboard.util.ExcelParserUtil;

@Service
public class DashboardServiceImpl implements DashboardService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ApplicationContext context;

	@Override
	public Response uploadExcel(MultipartFile file) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		try {

			List<ExcelRowModel> rows = ExcelParserUtil.parseExcel(file);

			for (ExcelRowModel model : rows) {

				Optional<Project> optionalProject = projectRepository.findByProjectName(model.getProjectName());

				Project project = optionalProject.orElseGet(Project::new);

				project.setProjectName(model.getProjectName());

				if (project.getPhases() == null) {

					project.setPhases(new ArrayList<>());
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

				if (activity == null) {

					activity = new Activity();

					activity.setActivityName(model.getActivityName());

					subTask.getActivities().add(activity);
				}

				activity.setEstimatedPeriodWeek(model.getEstimatedPeriodWeek());

				activity.setPlannedStartDate(model.getPlannedStartDate());

				activity.setPlannedEndDate(model.getPlannedEndDate());

				activity.setActualStartDate(model.getActualStartDate());

				activity.setActualEndDate(model.getActualEndDate());

				activity.setActualPeriodWeek(model.getActualPeriodWeek());

				activity.setProgress(model.getProgress());

				activity.setExecutionStatus(model.getExecutionStatus());

				activity.setScheduleHealth(model.getScheduleHealth());

				projectRepository.save(project);
			}

			return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
					"Excel Uploaded Successfully", rows);
		} catch (Exception e) {

			e.printStackTrace();

			throw new ReadExcelException("EXCEL_READ_ERROR", "Error while saving Excel into db: " + e.getMessage());
		}
	}

	@Override
	public Response getAllProjects() {
		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);
		List<Project> projects = projectRepository.findAll();
		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Projects Fetched Successfully", projects);
	}

	@Override
	public ByteArrayInputStream exportExcel(String projectName) {

		try {

			Project project = projectRepository.findByProjectName(projectName)
					.orElseThrow(() -> new RuntimeException("Project not found"));

			ClassPathResource resource = new ClassPathResource("templates/Project_Template.xlsx");

			Workbook workbook = WorkbookFactory.create(resource.getInputStream());

			Sheet sheet = workbook.getSheet("Project schedule");

			int templateRow = 7; // Excel Row 8
			int currentRow = 7;

			int srNo = 1;

			for (Phase phase : project.getPhases()) {

				for (Milestone milestone : phase.getMilestones()) {

					for (Task task : milestone.getTasks()) {

						for (Subtask subTask : task.getSubTasks()) {

							for (Activity activity : subTask.getActivities()) {

								Row row;

								if (currentRow == templateRow) {

									row = sheet.getRow(templateRow);

								} else {

									row = copyTemplateRow(sheet, templateRow, currentRow);
								}

								setCell(row, 0, ((srNo++)*100));

								setCell(row, 1, phase.getPhaseName());

								setCell(row, 2, milestone.getMilestoneName());

								setCell(row, 3, task.getTaskName());

								setCell(row, 4, subTask.getSubTaskName());

								setCell(row, 5, activity.getActivityName());

								setCell(row, 6, ""); // Owner

								setCell(row, 7, activity.getEstimatedPeriodWeek());

								setDate(row, 8, activity.getPlannedStartDate());

								setDate(row, 9, activity.getPlannedEndDate());

								setDate(row, 10, activity.getActualStartDate());

								setDate(row, 11, activity.getActualEndDate());

								setCell(row, 13, activity.getProgress());

								currentRow++;
							}
						}
					}
				}
			}

			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

			evaluator.evaluateAll();

			ByteArrayOutputStream output = new ByteArrayOutputStream();

			workbook.write(output);

			workbook.close();

			return new ByteArrayInputStream(output.toByteArray());

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while exporting excel", e);
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

	private void setCell(Row row, int column, String value) {

		Cell cell = row.getCell(column);

		if (cell == null) {

			cell = row.createCell(column);
		}

		cell.setCellValue(value == null ? "" : value);
	}

	private void setCell(Row row, int column, Integer value) {

		Cell cell = row.getCell(column);

		if (cell == null) {

			cell = row.createCell(column);
		}

		if (value != null) {

			cell.setCellValue(value/100.0);
		}
	}

	private void setCell(Row row, int column, Double value) {

		Cell cell = row.getCell(column);

		if (cell == null) {

			cell = row.createCell(column);
		}

		if (value != null) {

			cell.setCellValue(value);
		}
	}

	private void setDate(Row row, int column, LocalDate date) {

		if (date == null) {
			return;
		}

		Cell cell = row.getCell(column);

		if (cell == null) {

			cell = row.createCell(column);
		}

		cell.setCellValue(java.sql.Date.valueOf(date));
	}
}
