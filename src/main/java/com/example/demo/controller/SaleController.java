package com.example.demo.controller;

import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.ExcelService;
import com.example.demo.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private PdfService pdfService;
    @Autowired private EmailService emailService;
    @Autowired private ExcelService excelService;

    @GetMapping
    public List<Sale> getAllSales() {
        return saleRepo.findAll();
    }

    @PostMapping
    public Sale createSale(@RequestBody Map<String, Object> payload) {
        Sale sale = new Sale();
        
        // 1. Capture Basic Info
        sale.setCashierName((String) payload.get("cashierName"));
        
        // 2. CAPTURE PAYMENT DETAILS
        sale.setPaymentMethod((String) payload.getOrDefault("paymentMethod", "Cash"));
        sale.setStatus((String) payload.getOrDefault("status", "Paid"));

        // 3. Process Items
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        for (Map<String, Object> itemData : items) {
            SaleItem item = new SaleItem();
            item.setProductId(((Number) itemData.get("productId")).longValue());
            item.setProductName((String) itemData.get("name"));
            item.setPrice(new BigDecimal(itemData.get("price").toString()));
            item.setQuantity(((Number) itemData.get("quantity")).intValue());
            
            // Decrease Stock
            productRepo.findById(item.getProductId()).ifPresent(p -> {
                p.setStock(p.getStock() - item.getQuantity());
                productRepo.save(p);
            });

            sale.addItem(item);
        }

        sale.calculateTotal();
        return saleRepo.save(sale);
    }

    @GetMapping("/{id}")
    public Sale getSale(@PathVariable Long id) {
        return saleRepo.findById(id).orElseThrow(() -> new RuntimeException("Sale not found"));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<InputStreamResource> generatePdf(@PathVariable Long id) {
        Sale sale = getSale(id);
        ByteArrayInputStream pdf = pdfService.generateInvoice(sale);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=invoice-" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    @PostMapping("/{id}/email")
    public ResponseEntity<Void> emailReceipt(@PathVariable Long id, @RequestParam String email) {
        Sale sale = getSale(id);
        emailService.sendReceiptWithAttachment(email, sale);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportSales() {
        List<Sale> sales = saleRepo.findAll();
        ByteArrayInputStream in = excelService.exportSalesToExcel(sales);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=sales_report.xlsx");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.ms-excel")).body(new InputStreamResource(in));
    }
}
