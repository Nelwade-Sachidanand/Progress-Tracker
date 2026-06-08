package com.dashboard.util;

import java.time.DayOfWeek;

import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.dashboard.common.ErrorCode;
import com.dashboard.exception.ValidationException;
import com.dashboard.model.ActivityModel;
import com.dashboard.model.ExcelRowModel;

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
	        cell.setBlank(); // Clear old value
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
			LocalDate actualStartDate, LocalDate actualEndDate) {

		LocalDate today = LocalDate.now();
		if (progress != null && progress >= 100) {

			if (actualEndDate != null && plannedEndDate != null && actualEndDate.isAfter(plannedEndDate)) {
				return "Delayed";
			}
			return "On Track";
		}

		if (plannedStartDate != null && today.isBefore(plannedStartDate)) {
			return "On Track";
		}

		if ((progress == null || progress == 0) && plannedEndDate != null && today.isAfter(plannedEndDate)) {
			return "Delayed";
		}

		if (actualStartDate != null && plannedStartDate != null && actualStartDate.isAfter(plannedStartDate)) {
			return "At Risk";
		}

		if (plannedEndDate != null && today.isAfter(plannedEndDate)) {
			return "Delayed";
		}
		return "On Track";
	}

	public static void validateRequest(ActivityModel request) {

		if (isBlank(request.getProjectName())) {
			throw new ValidationException(ErrorCode.PROJECT_NAME_REQUIRED, "Project name is required");
		}

		if (isBlank(request.getPhaseName())) {
			throw new ValidationException(ErrorCode.PHASE_NAME_REQUIRED, "Phase name is required");
		}

		if (isBlank(request.getMilestoneName())) {
			throw new ValidationException(ErrorCode.MILESTONE_REQUIRED, "Milestone name is required");
		}

		if (isBlank(request.getTaskName())) {
			throw new ValidationException(ErrorCode.TASK_REQUIRED, "Task name is required");
		}

		if (isBlank(request.getSubTaskName())) {
			throw new ValidationException(ErrorCode.SUBTASK_REQUIRED, "SubTask name is required");
		}

		if (isBlank(request.getActivityName())) {
			throw new ValidationException(ErrorCode.ACTIVITY_NAME_REQUIRED, "Activity name is required");
		}
		if (request.getPlannedStartDate() != null && request.getPlannedEndDate() != null
				&& request.getPlannedStartDate().isAfter(request.getPlannedEndDate())) {
			throw new ValidationException(ErrorCode.INVALID_PLANNED_DATES,
					"Planned start date cannot be after planned end date");
		}

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

		if (request.getProgress() != null && (request.getProgress() < 0 || request.getProgress() > 100)) {
			throw new ValidationException(ErrorCode.INVALID_PROGRESS, "Progress must be between 0 and 100");
		}
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

	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
