package com.example.demo.controller;

import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.PdfService;
import com.example.demo.service.SaleService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*; // 🟢 APACHE POI IMPORTS
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // 🟢 FOR EXCEL
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired private SaleRepository saleRepo;
    @Autowired private SaleService saleService;
    @Autowired private PdfService pdfService;
    @Autowired private EmailService emailService;

    // --- 1. CREATE SALE ---
    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody Map<String, Object> saleData) {
        try {
            Sale newSale = saleService.createSale(saleData);
            return ResponseEntity.ok(Map.of("id", newSale.getId(), "message", "Sale successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing sale: " + e.getMessage());
        }
    }

    // --- 2. DOWNLOAD PDF ---
    @GetMapping("/{id}/pdf")
    public void downloadReceipt(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Sale sale = saleRepo.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
        
        ByteArrayInputStream pdfStream = pdfService.generateInvoice(sale);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice-" + id + ".pdf");
        pdfStream.transferTo(response.getOutputStream());
    }

    // --- 3. SHARE INVOICE ---
    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareInvoice(@PathVariable Long id, @RequestParam String email) {
        try {
            Sale sale = saleRepo.findByIdWithItems(id)
                    .orElseThrow(() -> new RuntimeException("Sale not found"));

            new Thread(() -> {
                try {
                    emailService.sendReceiptWithAttachment(email, sale);
                } catch (Exception e) {
                    System.err.println("❌ Error sending email: " + e.getMessage());
                }
            }).start();

            return ResponseEntity.ok("Email is being sent to " + email);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initiate email: " + e.getMessage());
        }
    }

    // --- 4. EXPORT EXCEL (✅ FIXED & ACTIVATED) ---
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSalesToExcel() throws IOException {
        List<Sale> sales = saleRepo.findAll();

        // 1. Create Workbook & Sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sales Data");

        // 2. Create Header
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Date", "Cashier", "Customer", "Payment", "Status", "Total Amount"};
        
        // Style for Header (Bold)
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // 3. Fill Data Rows
        int rowIdx = 1;
        for (Sale sale : sales) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(sale.getId());
            row.createCell(1).setCellValue(sale.getDate().toString());
            row.createCell(2).setCellValue(sale.getCashierName() != null ? sale.getCashierName() : "Unknown");
            row.createCell(3).setCellValue(sale.getCustomerPhone() != null ? sale.getCustomerPhone() : "-");
            row.createCell(4).setCellValue(sale.getPaymentMethod());
            row.createCell(5).setCellValue(sale.getStatus());
            row.createCell(6).setCellValue(sale.getTotalAmount().doubleValue());
        }

        // 4. Auto-size columns for readability
        for(int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 5. Write to stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        // 6. Return as File Download
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_report.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out.toByteArray());
    }
}