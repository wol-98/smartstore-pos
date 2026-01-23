package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.PredictionService; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private PredictionService predictionService; // 👈 Inject the Brain

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        // --- 1. Fetch All Data (For General Stats) ---
        List<Sale> allSales = saleRepo.findAll();
        
        // Date Filtering Logic
        if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
            try {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                allSales = allSales.stream()
                        .filter(s -> s.getDate().isAfter(start) && s.getDate().isBefore(end))
                        .collect(Collectors.toList());
            } catch (Exception e) { System.err.println("Date parse error"); }
        }

        // --- 2. Calculate KPI Cards ---
        BigDecimal totalRevenue = allSales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Low Stock Count
        long lowStockCount = productRepo.findAll().stream()
                .filter(p -> p.getStock() <= 5).count();

        // Top Products
        Map<String, Integer> productSales = allSales.stream()
                .flatMap(s -> s.getItems().stream()) 
                .collect(Collectors.groupingBy(
                        SaleItem::getProductName, 
                        Collectors.summingInt(SaleItem::getQuantity)
                ));

        Map<String, Integer> topProducts = productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // Top Customers
        List<Customer> topCustomers = new ArrayList<>();
        try { topCustomers = customerRepo.findTop5ByOrderByPointsDesc(); } catch (Exception e) {}


        // --- 3. 🧠 REAL AI FORECASTING ---
        // Fetch only last 7 days for the algorithm (Efficiency)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Sale> recentSales = saleRepo.findByDateAfter(sevenDaysAgo);

        // Data Prep: Group Sales by Date -> Total Quantity Sold that day
        Map<LocalDate, Integer> dailyTotals = recentSales.stream()
            .collect(Collectors.groupingBy(
                s -> s.getDate().toLocalDate(),
                Collectors.summingInt(s -> s.getItems().stream().mapToInt(SaleItem::getQuantity).sum())
            ));

        // Call the AI Engine
        int predictedSales = predictionService.predictNextDaySales(dailyTotals);
        
        // --- 4. Assemble Response ---
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", allSales.size());
        stats.put("lowStockCount", lowStockCount);
        stats.put("topProducts", topProducts);
        stats.put("topCustomers", topCustomers);
        
        // AI Data
        stats.put("predictedSales", predictedSales);
        stats.put("aiStatus", predictedSales > 0 ? "ML Powered" : "Gathering Data"); 

        return stats;
    }
}