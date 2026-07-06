package com.novillex.progresstracker.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.exception.ReadExcelException;
import com.novillex.progresstracker.model.ExcelRowModel;

public class ExcelParserUtil {
	private static final Logger logger = LoggerFactory.getLogger(ExcelParserUtil.class);

	public static List<ExcelRowModel> parseExcel(MultipartFile file) {

		List<ExcelRowModel> rowList = new ArrayList<>();

		try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {

			for (Name name : workbook.getAllNames()) {
				System.out.println("Name: " + name.getNameName() + " RefersTo: " + name.getRefersToFormula());
			}
			logger.info("Excel parsing started. File: {}", file.getOriginalFilename());

			Sheet sheet = workbook.getSheet("FinWiz_Project_Schedule");

			if (sheet == null) {
				throw new ReadExcelException(ErrorCode.EXCEL_READ_ERROR,
						"Sheet 'FinWiz_Project_Schedule' not found in Excel");
			}
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

			ExcelRowModel model;

			String projectName = sheet.getRow(2).getCell(3).getStringCellValue();

			String bankName = sheet.getRow(1).getCell(3).getStringCellValue();
			String managerName = sheet.getRow(3).getCell(3).getStringCellValue();

			logger.info("Excel metadata loaded. Project: {}, Bank: {}, Manager: {}", projectName, bankName,
					managerName);

			for (int i = 7; i <= sheet.getLastRowNum(); i++) {

				Row row = sheet.getRow(i);

				if (row == null)
					break;
				String phaseName = ReadUtil.getString(row.getCell(1), evaluator);

				if (phaseName == null || phaseName.trim().isEmpty()) {
					break;
				}
				model = new ExcelRowModel();
				model.setBankName(bankName);
				model.setProjectManager(managerName);
				model.setProjectName(projectName);
				model.setPhaseName(phaseName); // Missing in your code
				model.setMilestoneName(ReadUtil.getString(row.getCell(2), evaluator));
				model.setTaskName(ReadUtil.getString(row.getCell(3), evaluator));
				model.setSubTaskName(ReadUtil.getString(row.getCell(4), evaluator));
				model.setActivityName(ReadUtil.getString(row.getCell(5), evaluator));
				model.setOwner(ReadUtil.getString(row.getCell(6), evaluator));
				model.setEstimatedPeriodWeek(ReadUtil.getDouble(row.getCell(7), evaluator));
				model.setPlannedStartDate(ReadUtil.getLocalDate(row.getCell(8)));
				model.setPlannedEndDate(ReadUtil.getLocalDate(row.getCell(9)));
				model.setActualStartDate(ReadUtil.getLocalDate(row.getCell(10)));
				model.setActualEndDate(ReadUtil.getLocalDate(row.getCell(11)));
//				model.setActualPeriodWeek(
//						ReadUtil.calculateActualPeriod(model.getActualStartDate(), model.getActualEndDate()));
				model.setActualPeriodWeek(ReadUtil.getDouble(row.getCell(12), evaluator));
				model.setProgress(ReadUtil.getInt(row.getCell(13), evaluator));

				model.setExecutionStatus(ReadUtil.getString(row.getCell(14), evaluator));

				model.setScheduleHealth(ReadUtil.getString(row.getCell(15), evaluator));
				/*
				 * Cell scheduleCell = row.getCell(15);
				 * 
				 * logger.info("Activity={}", model.getActivityName());
				 * 
				 * if (scheduleCell != null) {
				 * 
				 * logger.info("CellType={}", scheduleCell.getCellType());
				 * 
				 * if (scheduleCell.getCellType() == CellType.FORMULA) {
				 * logger.info("Formula={}", scheduleCell.getCellFormula()); }
				 * 
				 * logger.info("CellToString={}", scheduleCell.toString()); }
				 * 
				 * model.setScheduleHealth(ReadUtil.getString(scheduleCell, evaluator));
				 */
				String remark = ReadUtil.getString(row.getCell(16), evaluator);

				model.setRemark(remark == null || remark.trim().isEmpty() ? null : remark.trim());
//				logger.info("ScheduleHealth={}", model.getScheduleHealth());
//				System.out.println(model);
				rowList.add(model);
			}

			workbook.close();
			logger.info("Excel parsing completed successfully. Total rows processed: {}", rowList.size());

			return rowList;
		} catch (Exception e) {
			logger.error("Failed to parse Excel file: {}", file.getOriginalFilename(), e);
			throw new ReadExcelException(ErrorCode.EXCEL_READ_ERROR, "Error while processing Excel: " + e.getMessage());
		}
	}
}