package com.dashboard.common.util;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import com.dashboard.model.ExcelRowModel;

public class ExcelParserUtil {

	public static List<ExcelRowModel> parseExcel(MultipartFile file) {

		List<ExcelRowModel> rowList = new ArrayList<>();

		try {

			InputStream inputStream = file.getInputStream();

			Workbook workbook = new XSSFWorkbook(inputStream);

			Sheet sheet = workbook.getSheetAt(1);
			
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

			for (int i = 8; i <= sheet.getLastRowNum(); i++) {

				Row row = sheet.getRow(i);

				if (row == null) {
					continue;
				}

				ExcelRowModel model = new ExcelRowModel();

				model.setPhaseName(getString(row.getCell(1),evaluator));

				model.setMilestoneName(getString(row.getCell(2), evaluator));

				model.setTaskName(getString(row.getCell(3), evaluator));

				model.setSubTaskName(getString(row.getCell(4), evaluator));

				model.setActivityName(getString(row.getCell(5), evaluator));

				model.setEstimatedPeriodWeek(getDouble(row.getCell(7), evaluator));

				model.setPlannedStartDate(getLocalDate(row.getCell(8)));

				model.setPlannedEndDate(getLocalDate(row.getCell(9)));

				model.setActualStartDate(getLocalDate(row.getCell(10)));

				model.setActualEndDate(getLocalDate(row.getCell(11)));
				
				model.setActualPeriodWeek(getDouble(row.getCell(12), evaluator));

				model.setProgress(getInt(row.getCell(13), evaluator));

				model.setExecutionStatus(getString(row.getCell(14), evaluator));

				model.setScheduleHealth(getString(row.getCell(15), evaluator));

				rowList.add(model);
			}

			workbook.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return rowList;
	}
	public static Double getDouble(Cell cell, FormulaEvaluator evaluator) {

	    if (cell == null) return null;
//	    System.out.println(cell.getCellType());
	    try {
	        switch (cell.getCellType()) {

	            case NUMERIC:
	                return cell.getNumericCellValue();

	            case FORMULA:
//	            	CellType cachedType = cell.getCachedFormulaResultType();
//	            	System.out.println(cell.getStringCellValue());
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
//		System.out.println(cell.getNumericCellValue());
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
//			System.out.println("cell type BLANK");
		    return null;
		}
	    try {

	        if (cell.getCellType() == CellType.NUMERIC &&
	            DateUtil.isCellDateFormatted(cell)) {

	            return cell.getLocalDateTimeCellValue().toLocalDate();
	        }

	        if (cell.getCachedFormulaResultType() == CellType.NUMERIC &&
	                DateUtil.isCellDateFormatted(cell)) {

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
		if(cell == null) return "";
//		System.out.println(cell.getCellType());
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
	
	public static Double calculateActualPeriod(LocalDate start, LocalDate end) {

	    try {
	        if (start == null || end == null) {
	            return null;
	        }
	        LocalDate s = start;
	        LocalDate e = end;

	        int workingDays = 0;

	        while (!s.isAfter(e)) {
	            if (s.getDayOfWeek() != DayOfWeek.SATURDAY &&
	                s.getDayOfWeek() != DayOfWeek.SUNDAY) {

	                workingDays++;
	            }
	            s = s.plusDays(1);
	        }

	        double weeks = workingDays / 5.0;
	        double rounded = Math.ceil(weeks * 2) / 2.0;

	        return rounded;

	    } catch (Exception e) {
//	        throw new ReadExcelException("Error while calculating actual period : ", e.getMessage());
	    }
		return null;
	}
}
