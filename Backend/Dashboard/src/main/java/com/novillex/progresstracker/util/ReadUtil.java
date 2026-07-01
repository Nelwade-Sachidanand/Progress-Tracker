package com.novillex.progresstracker.util;

import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

public class ReadUtil {

	public static Double getDouble(Cell cell, FormulaEvaluator evaluator) {

		if (cell == null) {
			return null;
		}

		try {

			switch (cell.getCellType()) {

			case NUMERIC:
				return cell.getNumericCellValue();

			case FORMULA:

				CellValue cellValue = evaluator.evaluate(cell);

				if (cellValue == null) {
					return null;
				}

				switch (cellValue.getCellType()) {

				case NUMERIC:
					return cellValue.getNumberValue();

				case STRING:
					String value = cellValue.getStringValue();
					return value.isBlank() ? null : Double.parseDouble(value);

				default:
					return null;
				}

			case STRING:
				String val = cell.getStringCellValue();
				return val.isBlank() ? null : Double.parseDouble(val);

			default:
				return null;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Integer getInt(Cell cell, FormulaEvaluator evaluator) {
		try {

			double val = cell.getNumericCellValue();

			return (int) Math.round(val * 100);

		} catch (Exception e) {
		}
		return null;
	}

	public static LocalDate getLocalDate(Cell cell) {

		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		try {

			if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {

				return cell.getLocalDateTimeCellValue().toLocalDate();
			}

			if (cell.getCachedFormulaResultType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {

				return cell.getLocalDateTimeCellValue().toLocalDate();
			}

			return null;

		} catch (Exception e) {
			System.out.println("error white getting date cell value ");
			e.printStackTrace();
		}
		return null;
	}

	public static String getString(Cell cell, FormulaEvaluator evaluator) {

		if (cell == null) {
			return "";
		}

		try {

			switch (cell.getCellType()) {

			case STRING:
				return cell.getStringCellValue().trim();

			case NUMERIC:
				return String.valueOf(cell.getNumericCellValue());

			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());

			case FORMULA:

				CellValue cellValue = evaluator.evaluate(cell);

				switch (cellValue.getCellType()) {

				case STRING:
					return cellValue.getStringValue().trim();

				case NUMERIC:
					return String.valueOf(cellValue.getNumberValue());

				case BOOLEAN:
					return String.valueOf(cellValue.getBooleanValue());

				default:
					return "";
				}

			default:
				return "";
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

//	public static Double calculateActualPeriod(LocalDate taskStart, LocalDate taskEnd) {
//		if (taskStart == null || taskEnd == null) {
//			return null;
//		}
//
//		int workingDays = 0;
//
//		LocalDate currentDate = taskStart;
//		while (!currentDate.isAfter(taskEnd)) {
//
//			DayOfWeek day = currentDate.getDayOfWeek();
//			if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
//				workingDays++;
//			}
//
//			currentDate = currentDate.plusDays(1);
//		}
//
//		return (double) workingDays / 5;
//	}
}
