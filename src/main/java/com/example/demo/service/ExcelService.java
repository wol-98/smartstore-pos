package com.example.demo.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.demo.model.Sale;

@Service
public class ExcelService {

    public ByteArrayInputStream exportSalesToExcel(List<Sale> sales) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales Report");

            // Header Row Style
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] columns = {"Sale ID", "Date", "Cashier", "Total Amount"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 1;
            for (Sale sale : sales) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sale.getId());
                row.createCell(1).setCellValue(sale.getDate().toString());
                row.createCell(2).setCellValue(sale.getCashierName());
                row.createCell(3).setCellValue(sale.getTotalAmount().doubleValue());
            }
            
            // Auto-size columns
            for(int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Fail to import data to Excel file: " + e.getMessage());
        }
    }
}