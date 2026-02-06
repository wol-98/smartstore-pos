package com.example.demo.controller;

import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.DashboardService;
import com.example.demo.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    // 1. Inject the Service (The Brain), NOT the Repositories
    @Autowired private DashboardService dashboardService;
    @Autowired private PredictionService predictionService;
    @Autowired private SaleRepository saleRepo; // Only needed for AI specific query

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        // 2. Handle Dates Cleanly
        LocalDate start;
        LocalDate end;

        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate);
            } else {
                start = LocalDate.now(); // Default to Today
            }

            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate);
            } else {
                end = LocalDate.now(); // Default to Today
            }
        } catch (Exception e) {
            // Fallback if user sends bad dates
            start = LocalDate.now();
            end = LocalDate.now();
        }

        // 3. Delegate to the Service (Reuse the crash-proof logic)
        return dashboardService.getDashboardStats(start, end);
    }

    @GetMapping("/stats/ai")
    public Map<String, Object> getAiStats() {
        // AI Logic can stay here or move to a service later
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Sale> recentSales = saleRepo.findByDateAfter(sevenDaysAgo);
        
        Map<LocalDate, Integer> dailyTotals = recentSales.stream()
            .filter(s -> s.getDate() != null && s.getItems() != null)
            .collect(Collectors.groupingBy(
                    s -> s.getDate().toLocalDate(), 
                    Collectors.summingInt(s -> s.getItems().stream().mapToInt(SaleItem::getQuantity).sum())
            ));
            
        int predictedSales = predictionService.predictNextDaySales(dailyTotals);
        return Map.of("predictedSales", predictedSales);
    }
}