package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AlertScheduler {

    @Autowired private ProductRepository productRepo;
    @Autowired private SaleRepository saleRepo;
    @Autowired private EmailService emailService;

    private final String ADMIN_EMAIL = "grabmeonly@gmail.com"; 

    // 🕒 Check every 30 seconds (For testing)
    // Once satisfied, change back to 14400000 (4 hours)
    @Scheduled(fixedRate = 14400000) 
    public void checkLowStock() {
        List<Product> products = productRepo.findAll();
        StringBuilder alertBody = new StringBuilder("<h3>⚠️ Low Stock Alert</h3><ul>");
        boolean needsEmail = false;
        List<Product> productsToUpdate = new ArrayList<>(); // List to save changes

        for (Product p : products) {
            if (p.getStock() == null) continue; // Skip null stock

            // CASE 1: Stock is Low AND We haven't alerted yet
            if (p.getStock() <= 5 && !p.getIsAlertSent()) {
                alertBody.append("<li><b>").append(p.getName())
                         .append("</b>: Only ").append(p.getStock()).append(" left!</li>");
                
                // Mark as alerted so we don't spam next time
                p.setIsAlertSent(true); 
                productsToUpdate.add(p);
                needsEmail = true;
            }
            
            // CASE 2: Stock Recovered (User restocked) -> Reset the flag
            else if (p.getStock() > 5 && p.getIsAlertSent()) {
                p.setIsAlertSent(false); // Reset so we alert again if it drops later
                productsToUpdate.add(p);
            }
        }
        alertBody.append("</ul><p>Please contact suppliers.</p>");

        // 1. Save the updated flags to database
        if (!productsToUpdate.isEmpty()) {
            productRepo.saveAll(productsToUpdate);
        }

        // 2. Send Email ONLY if there are NEW alerts
        if (needsEmail) {
            System.out.println("⚠️ Found NEW low stock items. Sending Email...");
            emailService.sendSimpleEmail(ADMIN_EMAIL, "⚠️ Inventory Warning - Action Needed", alertBody.toString());
        } else {
            System.out.println("✅ Checked Stock: No new alerts.");
        }
    }

    // 🌙 Task 2: Daily Sales Report at 10:00 PM
    @Scheduled(cron = "0 0 22 * * ?") 
    public void sendDailyReport() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        List<Sale> todaysSales = saleRepo.findByDateBetween(startOfDay, endOfDay);
        
        BigDecimal totalRevenue = todaysSales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String subject = "📊 Daily Closing Report - " + LocalDate.now();
        String body = "<h3>End of Day Summary</h3>" +
                      "<p><b>Total Sales:</b> " + todaysSales.size() + "</p>" +
                      "<p><b>Total Revenue:</b> Rs. " + totalRevenue + "</p>" +
                      "<br><i>Good job today!</i>";

        emailService.sendSimpleEmail(ADMIN_EMAIL, subject, body);
        System.out.println("✅ Sent Daily Report.");
    }
}