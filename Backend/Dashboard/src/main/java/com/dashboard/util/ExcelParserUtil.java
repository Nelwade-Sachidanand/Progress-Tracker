package com.dashboard.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import com.dashboard.exception.ReadExcelException;
import com.dashboard.model.ExcelRowModel;

public class ExcelParserUtil {

	public static List<ExcelRowModel> parseExcel(MultipartFile file) {

		List<ExcelRowModel> rowList = new ArrayList<>();

		try {

			InputStream inputStream = file.getInputStream();

			Workbook workbook = new XSSFWorkbook(inputStream);

			Sheet sheet = workbook.getSheet("Project Schedule");

			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			
			ExcelRowModel model ;
			
			String projectName = sheet.getRow(2).getCell(3).getStringCellValue();
//			System.out.println(projectName);
			

			for (int i = 7; i <= sheet.getLastRowNum(); i++) {

				Row row = sheet.getRow(i);

				if (row == null) continue;

				model = new ExcelRowModel();
				
				model.setProjectName(projectName);

				model.setPhaseName(ReadUtil.getString(row.getCell(1), evaluator));

				model.setMilestoneName(ReadUtil.getString(row.getCell(2), evaluator));

				model.setTaskName(ReadUtil.getString(row.getCell(3), evaluator));

				model.setSubTaskName(ReadUtil.getString(row.getCell(4), evaluator));

				model.setActivityName(ReadUtil.getString(row.getCell(5), evaluator));

				model.setEstimatedPeriodWeek(ReadUtil.getDouble(row.getCell(7), evaluator));

				model.setPlannedStartDate(ReadUtil.getLocalDate(row.getCell(8)));

				model.setPlannedEndDate(ReadUtil.getLocalDate(row.getCell(9)));

				model.setActualStartDate(ReadUtil.getLocalDate(row.getCell(10)));

				model.setActualEndDate(ReadUtil.getLocalDate(row.getCell(11)));

				model.setActualPeriodWeek(ReadUtil.calculateActualPeriod(model.getActualStartDate(),model.getActualEndDate()));

				model.setProgress(ReadUtil.getInt(row.getCell(13), evaluator));

				model.setExecutionStatus(ReadUtil.getString(row.getCell(14), evaluator));

				model.setScheduleHealth(ReadUtil.getString(row.getCell(15), evaluator));

				rowList.add(model);
			}

			workbook.close();
			
			return rowList;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ReadExcelException("EXCEL_READ_ERROR", "Error while processing Excel: " + e.getMessage());
		}
	}
}