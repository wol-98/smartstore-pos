package com.example.demo.controller;

import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.ExcelService;
import com.example.demo.service.PdfService;
import com.example.demo.service.SaleService; // 👈 NEW IMPORT
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired private SaleRepository saleRepo;
    @Autowired private SaleService saleService; // 👈 Inject the new Service
    
    @Autowired private PdfService pdfService;
    @Autowired private EmailService emailService;
    @Autowired private ExcelService excelService;

    // --- 1. CREATE SALE (REFACTORED) ---
    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody Map<String, Object> saleData) {
        try {
            // 🧠 All the heavy logic is now hidden inside this one line
            Sale newSale = saleService.processSale(saleData);
            return ResponseEntity.ok(newSale);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing sale: " + e.getMessage());
        }
    }

    // --- 2. DOWNLOAD PDF (Unchanged) ---
    @GetMapping("/{id}/pdf")
    public void downloadReceipt(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Sale sale = saleRepo.findById(id).orElseThrow(() -> new RuntimeException("Sale not found"));
        ByteArrayInputStream pdfStream = pdfService.generateInvoice(sale);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=invoice-" + id + ".pdf");
        org.apache.commons.io.IOUtils.copy(pdfStream, response.getOutputStream());
    }

    // --- 3. SEND EMAIL (Unchanged) ---
    @PostMapping("/{id}/email")
    public ResponseEntity<?> sendReceiptEmail(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            Sale sale = saleRepo.findById(id).orElseThrow(() -> new RuntimeException("Sale not found"));
            String targetEmail = (body != null && body.containsKey("email")) ? body.get("email") : "grabmeonly@gmail.com";
            emailService.sendReceiptWithAttachment(targetEmail, sale);
            return ResponseEntity.ok().body("Email sent successfully to " + targetEmail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sending email: " + e.getMessage());
        }
    }

    // --- 4. EXPORT EXCEL (Unchanged) ---
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportSalesToExcel() {
        List<Sale> sales = saleRepo.findAll();
        ByteArrayInputStream in = excelService.exportSalesToExcel(sales);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=sales-report.xlsx");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel")).body(new InputStreamResource(in));
    }
}