package com.dashboard.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.dashboard.model.ActivityModel;
import com.dashboard.service.ExcelService;
import com.dashboard.util.WriteUtil;

@Service
public class ExcelServiceImpl implements ExcelService {

	@Override
	public byte[] generateExcel(List<ActivityModel> reports) {
		
		ClassPathResource resource = new ClassPathResource("templates/Project_Template.xlsx");

		try (Workbook workbook = WorkbookFactory.create(resource.getInputStream())) {

			Sheet sheet = workbook.getSheet("Project schedule");
//			sheet.getRow(1).getCell(3).setCellValue(reports.get(0).getBankName());
			sheet.getRow(2).getCell(3).setCellValue(reports.get(0).getProjectName());
//			sheet.getRow(3).getCell(3).setCellValue(project.getProjectManager());
			
			int templateRow = 7;
			int currentRow = templateRow;

			int srNo = 1;

			int rowNum = 1;

			for (ActivityModel report : reports) {

				Row row;
				if (currentRow == templateRow) {
					row = sheet.getRow(templateRow);
				} else {

					row = copyTemplateRow(sheet, templateRow, currentRow);
				}

				WriteUtil.setCell(row, 0, (srNo++) * 100);
				WriteUtil.setCell(row, 1, report.getPhaseName());
				WriteUtil.setCell(row, 2, report.getMilestoneName());
				WriteUtil.setCell(row, 3, report.getTaskName());
				WriteUtil.setCell(row, 4, report.getSubTaskName());
				WriteUtil.setCell(row, 5, report.getActivityName());
				WriteUtil.setCell(row, 6, "");
				WriteUtil.setCell(row, 7, report.getEstimatedPeriodWeek());
				WriteUtil.setDate(row, 8, report.getPlannedStartDate());
				WriteUtil.setDate(row, 9, report.getPlannedEndDate());
				WriteUtil.setDate(row, 10, report.getActualStartDate());
				WriteUtil.setDate(row, 11, report.getActualEndDate());
				WriteUtil.setCell(row, 13, report.getProgress());

				currentRow++;
			}
			
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateAll();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			workbook.write(output);

			workbook.write(output);

			return output.toByteArray();

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to generate Excel", e);
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

}
