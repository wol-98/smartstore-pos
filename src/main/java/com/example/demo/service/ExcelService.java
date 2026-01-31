package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ExcelService {

    // --- 1. EXISTING EXPORT METHOD ---
    public ByteArrayInputStream exportSalesToExcel(List<Sale> sales) {
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Sales Data");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Sale ID", "Date", "Cashier", "Payment Method", "Status", "Total", "Items"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

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
                
                if (sale.getDate() != null) row.createCell(1).setCellValue(sale.getDate().toString());
                else row.createCell(1).setCellValue("N/A");
                
                row.createCell(2).setCellValue(sale.getCashierName());
                row.createCell(3).setCellValue(sale.getPaymentMethod());
                row.createCell(4).setCellValue(sale.getStatus());
                
                if (sale.getTotalAmount() != null) row.createCell(5).setCellValue(sale.getTotalAmount().doubleValue());
                else row.createCell(5).setCellValue(0.0);

                StringBuilder itemsStr = new StringBuilder();
                if (sale.getItems() != null) {
                    for (SaleItem item : sale.getItems()) {
                        itemsStr.append(item.getProductName()).append(" (x").append(item.getQuantity()).append("), ");
                    }
                }
                row.createCell(6).setCellValue(itemsStr.toString());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to export Excel data: " + e.getMessage());
        }
    }

    // --- 2. 🆕 IMPORT METHOD (UPDATED FOR NEW PRODUCT CLASS) ---
    public List<Product> parseExcelFile(MultipartFile file) {
        List<Product> products = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // Skip Header Row (Row 0)
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Product product = new Product();
                
                // Reading Columns: Name | Brand | Category | Selling Price | Stock
                if(currentRow.getCell(0) != null) product.setName(currentRow.getCell(0).getStringCellValue());
                if(currentRow.getCell(1) != null) product.setBrand(currentRow.getCell(1).getStringCellValue());
                if(currentRow.getCell(2) != null) product.setCategory(currentRow.getCell(2).getStringCellValue());
                
                // 🚨 UPDATED: Handle BigDecimal Price
                if(currentRow.getCell(3) != null) {
                    double sellPriceVal = currentRow.getCell(3).getNumericCellValue();
                    product.setSellingPrice(BigDecimal.valueOf(sellPriceVal));
                    
                    // 🚀 AUTO-CALCULATE Buying Price (85% of Selling Price) to ensure non-null constraint
                    product.setBuyingPrice(BigDecimal.valueOf(sellPriceVal * 0.85));
                }

                if(currentRow.getCell(4) != null) product.setStock((int) currentRow.getCell(4).getNumericCellValue());
                
                // Set Defaults
                product.setDiscount(0.0);
                product.setMinStock(5); 

                products.add(product);
                rowNumber++;
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        return products;
    }
}