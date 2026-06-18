package com.novillex.progresstracker.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

public class ReadUtil {
	
	public static Double getDouble(Cell cell, FormulaEvaluator evaluator) {

		if (cell == null)
			return null;
		try {
			switch (cell.getCellType()) {

			case NUMERIC:
				return cell.getNumericCellValue();

			case FORMULA:
				return Double.parseDouble(cell.getStringCellValue());

			case STRING:
				String val = cell.getStringCellValue();
				return val.isEmpty() ? null : Double.parseDouble(val);

			default:
				return null;
			}
		} catch (Exception e) {
//	    	throw new ReadExcelException("Error while reading in getDouble() : ", e.getMessage());
		}
		return null;
	}

	public static Integer getInt(Cell cell, FormulaEvaluator evaluator) {
		try {

			double val = cell.getNumericCellValue();

			return (int) Math.round(val * 100);

		} catch (Exception e) {
//		    	throw new ReadExcelException("Error in gentInt() method", e.getMessage());
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
//	    	throw new ReadExcelException("Error while reading in getLocalDate() : ", e.getMessage());
		}
		return null;
	}

	public static String getString(Cell cell, FormulaEvaluator evaluator) {
		if (cell == null)
			return "";
		try {
			switch (cell.getCellType()) {

			case STRING:
				String str = cell.getStringCellValue().trim();
				return str;

			case FORMULA:

				CellType cachedType = cell.getCachedFormulaResultType();

				if (cachedType == CellType.STRING) {
					String val = cell.getStringCellValue().trim();
					return val;
				}

				return "";

			default:
				return "";
			}

		} catch (Exception e) {
			e.printStackTrace();
//	    	throw new ReadExcelException("Error while reading in getString() : ", e.getMessage());
		}
		return null;
	}

	public static Double calculateActualPeriod(LocalDate taskStart, LocalDate taskEnd) {
		if (taskStart == null || taskEnd == null) {
			return  null;
		}

		int workingDays = 0;

		LocalDate currentDate = taskStart;
		while (!currentDate.isAfter(taskEnd)) {

			DayOfWeek day = currentDate.getDayOfWeek();
			if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
				workingDays++;
			}

			currentDate = currentDate.plusDays(1);
		}

		return (double) workingDays / 5;
	}
}
