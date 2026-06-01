package com.dashboard.util;

import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

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

		if (value != null) {

			cell.setCellValue(value / 100.0);
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
}
