package com.dashboard.util;

import java.time.DayOfWeek;

import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.dashboard.exception.ValidationException;
import com.dashboard.model.ActivityModel;

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

		if (date == null) {
			return;
		}

		Cell cell = row.getCell(column);

		if (cell == null) {

			cell = row.createCell(column);
		}

		cell.setCellValue(java.sql.Date.valueOf(date));
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

	public static  String calculateExecutionStatus(Integer progress) {

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
	
	
	public static  void validateRequest(ActivityModel request) {

		if (isBlank(request.getProjectName())) {
			throw new ValidationException("VAL_001", "Project name is required");
		}

		if (isBlank(request.getPhaseName())) {
			throw new ValidationException("VAL_002", "Phase name is required");
		}

		if (request.getTaskName() != null && request.getMilestoneName() == null) {
			throw new ValidationException("VAL_003", "Milestone name is required before task creation");
		}

		if (request.getSubTaskName() != null && request.getTaskName() == null) {
			throw new ValidationException("VAL_004", "Task name is required before subtask creation");
		}

		if (request.getActivityName() != null && request.getSubTaskName() == null) {
			throw new ValidationException("VAL_005", "SubTask name is required before activity creation");
		}
		if (isBlank(request.getActivityName())) {
			throw new ValidationException("VAL_015", "Activity name is required");
		}

		if (request.getPlannedStartDate() != null && request.getPlannedEndDate() != null
				&& request.getPlannedStartDate().isAfter(request.getPlannedEndDate())) {
			throw new ValidationException("VAL_006", "Planned start date cannot be after planned end date");
		}

		if (request.getActualStartDate() != null && request.getActualEndDate() != null
				&& request.getActualStartDate().isAfter(request.getActualEndDate())) {
			throw new ValidationException("VAL_007", "Actual start date cannot be after actual end date");
		}

		if (request.getActualStartDate() != null && request.getPlannedStartDate() == null) {
			throw new ValidationException("VAL_008", "Planned start date is required before actual start date");
		}

		if (request.getActualEndDate() != null && request.getActualStartDate() == null) {
			throw new ValidationException("VAL_009", "Actual start date is required before actual end date");
		}

		if (request.getProgress() != null && (request.getProgress() < 0 || request.getProgress() > 100)) {
			throw new ValidationException("VAL_010", "Progress must be between 0 and 100");
		}
		if (request.getEstimatedPeriodWeek() != null && request.getEstimatedPeriodWeek() <= 0) {
			throw new ValidationException("VAL_011", "Estimated period week must be greater than zero");
		}
	}

	public static  boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}
