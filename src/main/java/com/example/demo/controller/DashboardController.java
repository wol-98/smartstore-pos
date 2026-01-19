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
    @Autowired private PredictionService predictionService; 

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Sale> sales = saleRepo.findAll();
        
        // 1. Date Filtering
        if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
            try {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                sales = sales.stream()
                        .filter(s -> s.getDate().isAfter(start) && s.getDate().isBefore(end))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("Date parse error, ignoring filter");
            }
        }

        // 2. Total Revenue
        BigDecimal totalRevenue = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Low Stock (Safe Method: Filter in Java)
        // Assumes "Low Stock" is anything 5 or less
        long lowStockCount = productRepo.findAll().stream()
                .filter(p -> p.getStock() <= 5)
                .count();

        // 4. Top Products Calculation
        Map<String, Integer> productSales = sales.stream()
                .flatMap(s -> s.getItems().stream()) 
                .collect(Collectors.groupingBy(
                        SaleItem::getProductName, 
                        Collectors.summingInt(SaleItem::getQuantity)
                ));

        Map<String, Integer> topProducts = productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        Map.Entry::getValue, 
                        (e1, e2) -> e1, 
                        LinkedHashMap::new
                ));

        // 5. Top Customers (Safe Fallback if repo method missing)
        List<Customer> topCustomers = new ArrayList<>();
        try {
            // If you implemented the custom query in CustomerRepository, this runs
            topCustomers = customerRepo.findTop5ByOrderByPointsDesc();
        } catch (Exception e) {
            // Fallback: Just return empty list to prevent crash
        }

        // 6. AI Data Prep
        List<Map<String, Object>> aiData = sales.stream()
            .map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("date", s.getDate().toLocalDate().toString());
                m.put("qty", s.getItems().stream().mapToInt(SaleItem::getQuantity).sum());
                return m;
            }).collect(Collectors.toList());

        // 7. AI Prediction
        int predictedSales = predictionService.getPredictedSales(aiData);
        String aiStatus = predictedSales > 0 ? "online" : "offline"; 

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", sales.size());
        stats.put("lowStockCount", lowStockCount);
        stats.put("topProducts", topProducts);
        stats.put("topCustomers", topCustomers);
        stats.put("predictedSales", predictedSales);
        stats.put("aiStatus", aiStatus); 

        return stats;
    }
}