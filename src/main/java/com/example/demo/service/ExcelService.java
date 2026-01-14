package com.example.demo.service;

import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelService {

    public ByteArrayInputStream exportSalesToExcel(List<Sale> sales) {
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Sales Data");

            // 1. Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Sale ID", "Date", "Cashier", "Payment Method", "Status", "Total", "Items"};
            
            // Style for Header (Bold)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // 2. Fill Data Rows
            int rowIdx = 1;
            for (Sale sale : sales) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(sale.getId());
                
                // SAFETY CHECK: Handle null dates to prevent crash
                if (sale.getCreatedAt() != null) {
                    row.createCell(1).setCellValue(sale.getCreatedAt().toString());
                } else {
                    row.createCell(1).setCellValue("N/A");
                }
                
                row.createCell(2).setCellValue(sale.getCashierName());
                row.createCell(3).setCellValue(sale.getPaymentMethod());
                row.createCell(4).setCellValue(sale.getStatus());
                row.createCell(5).setCellValue(sale.getTotal().doubleValue());

                // Format the items list into a single string
                StringBuilder itemsStr = new StringBuilder();
                if (sale.getItems() != null) {
                    for (SaleItem item : sale.getItems()) {
                        itemsStr.append(item.getProductName())
                               .append(" (x").append(item.getQuantity()).append("), ");
                    }
                }
                row.createCell(6).setCellValue(itemsStr.toString());
            }

            // 3. Write to Memory (NOT Disk)
            workbook.write(out);
            
            // 4. Return as Stream
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to export Excel data: " + e.getMessage());
        }
    }
}