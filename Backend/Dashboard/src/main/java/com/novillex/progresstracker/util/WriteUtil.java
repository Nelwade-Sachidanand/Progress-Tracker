package com.novillex.progresstracker.util;

import java.time.DayOfWeek;

import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.model.ActivityModel;
import com.novillex.progresstracker.model.ActivityUpdateRequestModel;
import com.novillex.progresstracker.model.ExcelRowModel;

public class WriteUtil {
	public static void setCell(Row row, int column, String value) {

		Cell cell = row.getCell(column);

		if (cell == null) {

			cell = row.createCell(column);
		}

		cell.setCellValue(value == null ? "" : value);
	}

	public static void setCell(Row row, int column, Integer value) {

		Cell cell = row.getCell(column);

		if (cell == null) {
			cell = row.createCell(column);
		}

		if (value != null && value != 0) {
			cell.setCellValue(value / 100.0);
		} else {
			cell.setBlank();
		}
	}

	public static void setCell(Row row, int column, Double value) {

		Cell cell = row.getCell(column);

		if (cell == null) {

			cell = row.createCell(column);
		}

		if (value != null) {

			cell.setCellValue(value);
		}
	}

	public static void setDate(Row row, int column, LocalDate date) {

		Cell cell = row.getCell(column);

		if (cell == null) {
			cell = row.createCell(column);
		}

		if (date != null) {
			cell.setCellValue(java.sql.Date.valueOf(date));
		} else {
			cell.setBlank();
		}
	}

	public static Double calculateActualPeriodWeek(LocalDate startDate, LocalDate endDate) {

		if (startDate == null || endDate == null) {
			return null;
		}

		long workingDays = 0;
		LocalDate date = startDate;
		while (!date.isAfter(endDate)) {
			DayOfWeek day = date.getDayOfWeek();
			if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {

				workingDays++;
			}

			date = date.plusDays(1);
		}

		return workingDays / 5.0;
	}

	public static String calculateExecutionStatus(Integer progress) {

		if (progress == null) {
			return "Not Started";
		}

		if (progress >= 100) {
			return "Completed";
		}

		if (progress == 0) {
			return "Not Started";
		}

		return "In Progress";
	}

	
	public static String calculateScheduleHealth(Integer progress, LocalDate plannedStartDate, LocalDate plannedEndDate,
			LocalDate actualStartDate, LocalDate actualEndDate, Double actualPeriodWeek) {

		LocalDate today = LocalDate.now();

		// IF($N>=1, IF($M>$K,"Delayed","On Track"))
		if (progress != null && progress >= 100) {

			if (actualPeriodWeek != null && actualStartDate != null) {

				double actualStartSerial = actualStartDate.toEpochDay();

				if (actualPeriodWeek > actualStartSerial) {
					return "Delayed";
				}
			}

			return "On Track";
		}

		// IF(TODAY()<$J,"On Track")
		if (plannedEndDate != null && today.isBefore(plannedEndDate)) {
			return "On Track";
		}

		// IF(AND($N=0,TODAY()>$K),"Delayed")
		if (progress != null && progress == 0 && actualStartDate != null && today.isAfter(actualStartDate)) {
			return "Delayed";
		}

		// IF(AND($L<>"",$L>$J),"At Risk")
		if (actualEndDate != null && plannedEndDate != null && actualEndDate.isAfter(plannedEndDate)) {
			return "At Risk";
		}

		// IF(TODAY()>$K,"Delayed")
		if (actualStartDate != null && today.isAfter(actualStartDate)) {
			return "Delayed";
		}

		return "On Track";
	}

	public static void validateRequest(ActivityModel request) {

		if (isBlank(request.getProjectId())) {
			throw new ValidationException(ErrorCode.PROJECT_ID_REQUIRED, "Project id is required");
		}

		// Phase
		if (request.isNewPhase()) {

			if (isBlank(request.getPhaseName())) {
				throw new ValidationException(ErrorCode.PHASE_NAME_REQUIRED, "Phase name is required");
			}

		} else {

			if (isBlank(request.getPhaseId())) {
				throw new ValidationException(ErrorCode.PHASE_ID_REQUIRED, "Phase id is required");
			}
		}

		// Milestone
		if (request.isNewMilestone()) {

			if (isBlank(request.getMilestoneName())) {
				throw new ValidationException(ErrorCode.MILESTONE_REQUIRED, "Milestone name is required");
			}

		} else {

			if (isBlank(request.getMilestoneId())) {
				throw new ValidationException(ErrorCode.MILESTONE_ID_REQUIRED, "Milestone id is required");
			}
		}

		// Task
		if (request.isNewTask()) {

			if (isBlank(request.getTaskName())) {
				throw new ValidationException(ErrorCode.TASK_REQUIRED, "Task name is required");
			}

		} else {

			if (isBlank(request.getTaskId())) {
				throw new ValidationException(ErrorCode.TASK_ID_REQUIRED, "Task id is required");
			}
		}

		// SubTask
		if (request.isNewSubTask()) {

			if (isBlank(request.getSubTaskName())) {
				throw new ValidationException(ErrorCode.SUBTASK_REQUIRED, "SubTask name is required");
			}

		} else {

			if (isBlank(request.getSubTaskId())) {
				throw new ValidationException(ErrorCode.SUBTASK_ID_REQUIRED, "SubTask id is required");
			}
		}

		// Activity
		if (isBlank(request.getActivityName())) {
			throw new ValidationException(ErrorCode.ACTIVITY_NAME_REQUIRED, "Activity name is required");
		}

		// Planned Dates
		if (request.getPlannedStartDate() != null && request.getPlannedEndDate() != null
				&& request.getPlannedStartDate().isAfter(request.getPlannedEndDate())) {

			throw new ValidationException(ErrorCode.INVALID_PLANNED_DATES,
					"Planned start date cannot be after planned end date");
		}

		// Actual Dates
		if (request.getActualStartDate() != null && request.getActualEndDate() != null
				&& request.getActualStartDate().isAfter(request.getActualEndDate())) {

			throw new ValidationException(ErrorCode.INVALID_ACTUAL_DATES,
					"Actual start date cannot be after actual end date");
		}

		if (request.getActualStartDate() != null && request.getPlannedStartDate() == null) {

			throw new ValidationException(ErrorCode.PLANNED_DATE_REQUIRED,
					"Planned start date is required before actual start date");
		}

		if (request.getActualEndDate() != null && request.getActualStartDate() == null) {

			throw new ValidationException(ErrorCode.ACTUAL_START_REQUIRED,
					"Actual start date is required before actual end date");
		}

		// Progress
		if (request.getProgress() != null && (request.getProgress() < 0 || request.getProgress() > 100)) {

			throw new ValidationException(ErrorCode.INVALID_PROGRESS, "Progress must be between 0 and 100");
		}

		// Estimated Weeks
		if (request.getEstimatedPeriodWeek() != null && request.getEstimatedPeriodWeek() <= 0) {

			throw new ValidationException(ErrorCode.ESTIMATED_PERIOD_INVALID,
					"Estimated period week must be greater than zero");
		}
	}

	public static void validateExcelRow(ExcelRowModel model) {

		if (isBlank(model.getBankName())) {
			throw new ValidationException(ErrorCode.BANK_NAME_REQUIRED, "Bank name is required");
		}

		if (isBlank(model.getProjectName())) {
			throw new ValidationException(ErrorCode.PROJECT_NAME_REQUIRED, "Project name is required");
		}

		if (isBlank(model.getPhaseName())) {
			throw new ValidationException(ErrorCode.PHASE_NAME_REQUIRED, "Phase name is required");
		}

		if (isBlank(model.getMilestoneName())) {
			throw new ValidationException(ErrorCode.MILESTONE_REQUIRED, "Milestone name is required");
		}

		if (isBlank(model.getTaskName())) {
			throw new ValidationException(ErrorCode.TASK_REQUIRED, "Task name is required");
		}

		if (isBlank(model.getSubTaskName())) {
			throw new ValidationException(ErrorCode.SUBTASK_REQUIRED, "SubTask name is required");
		}

		if (isBlank(model.getActivityName())) {
			throw new ValidationException(ErrorCode.ACTIVITY_NAME_REQUIRED, "Activity name is required");
		}

		if (model.getEstimatedPeriodWeek() == null || model.getEstimatedPeriodWeek() <= 0) {

			throw new ValidationException(ErrorCode.ESTIMATED_PERIOD_INVALID,
					"Estimated period week must be greater than zero");
		}

		if (model.getPlannedStartDate() == null) {
			throw new ValidationException(ErrorCode.PLANNED_DATE_REQUIRED, "Planned start date is required");
		}

		if (model.getPlannedEndDate() == null) {
			throw new ValidationException(ErrorCode.PLANNED_DATE_REQUIRED, "Planned end date is required");
		}

		if (model.getPlannedStartDate() != null && model.getPlannedEndDate() != null
				&& model.getPlannedStartDate().isAfter(model.getPlannedEndDate())) {

			throw new ValidationException(ErrorCode.INVALID_PLANNED_DATES,
					"Planned start date cannot be after planned end date");
		}

		if (model.getActualStartDate() != null && model.getActualEndDate() != null
				&& model.getActualStartDate().isAfter(model.getActualEndDate())) {

			throw new ValidationException(ErrorCode.INVALID_ACTUAL_DATES,
					"Actual start date cannot be after actual end date");
		}

		if (model.getActualEndDate() != null && model.getActualStartDate() == null) {

			throw new ValidationException(ErrorCode.ACTUAL_START_REQUIRED,
					"Actual start date is required before actual end date");
		}

		if (model.getProgress() == null) {
			throw new ValidationException(ErrorCode.INVALID_PROGRESS, "Progress is required");
		}

		if (model.getProgress() < 0 || model.getProgress() > 100) {
			throw new ValidationException(ErrorCode.INVALID_PROGRESS, "Progress must be between 0 and 100");
		}
	}

	public static void validateUpdateRequest(ActivityUpdateRequestModel request) {

		if (isBlank(request.getProjectId())) {
			throw new ValidationException(ErrorCode.PROJECT_ID_REQUIRED, "Project id is required");
		}

		if (isBlank(request.getPhaseId())) {
			throw new ValidationException(ErrorCode.PHASE_ID_REQUIRED, "Phase id is required");
		}

		if (isBlank(request.getMilestoneId())) {
			throw new ValidationException(ErrorCode.MILESTONE_ID_REQUIRED, "Milestone id is required");
		}

		if (isBlank(request.getTaskId())) {
			throw new ValidationException(ErrorCode.TASK_ID_REQUIRED, "Task id is required");
		}

		if (isBlank(request.getSubTaskId())) {
			throw new ValidationException(ErrorCode.SUBTASK_ID_REQUIRED, "SubTask id is required");
		}

		if (isBlank(request.getActivityId())) {
			throw new ValidationException(ErrorCode.ACTIVITY_ID_REQUIRED, "Activity id is required");
		}

		if (isBlank(request.getActivityName())) {
			throw new ValidationException(ErrorCode.ACTIVITY_NAME_REQUIRED, "Activity name is required");
		}

		if (request.getEstimatedPeriodWeek() == null || request.getEstimatedPeriodWeek() <= 0) {
			throw new ValidationException(ErrorCode.ESTIMATED_PERIOD_INVALID,
					"Estimated period week must be greater than zero");
		}

		// Planned dates
		if (request.getPlannedStartDate() == null) {
			throw new ValidationException(ErrorCode.PLANNED_DATE_REQUIRED, "Planned start date is required");
		}

		if (request.getPlannedEndDate() == null) {
			throw new ValidationException(ErrorCode.PLANNED_DATE_REQUIRED, "Planned end date is required");
		}

		if (request.getPlannedStartDate().isAfter(request.getPlannedEndDate())) {
			throw new ValidationException(ErrorCode.INVALID_PLANNED_DATES,
					"Planned start date cannot be after planned end date");
		}

		// Progress validation
		if (request.getProgress() == null) {
			throw new ValidationException(ErrorCode.INVALID_PROGRESS, "Progress is required");
		}

		if (request.getProgress() < 0 || request.getProgress() > 100) {
			throw new ValidationException(ErrorCode.INVALID_PROGRESS, "Progress must be between 0 and 100");
		}

		// If progress is entered, actual dates become mandatory
		if (request.getProgress() > 0 && request.getActualStartDate() == null) {
				throw new ValidationException(ErrorCode.ACTUAL_START_REQUIRED, "Actual start date is required");
		}

		// Actual date validation
		if (request.getActualStartDate() != null && request.getActualEndDate() != null
				&& request.getActualStartDate().isAfter(request.getActualEndDate())) {

			throw new ValidationException(ErrorCode.INVALID_ACTUAL_DATES,
					"Actual start date cannot be after actual end date");
		}

		// Completed activity
		if (request.getProgress() == 100 && request.getActualEndDate() == null) {
			throw new ValidationException(ErrorCode.ACTUAL_END_REQUIRED,
					"Actual end date is required when progress is 100%");
		}

	}

	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
