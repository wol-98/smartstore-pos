package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.service.PredictionService; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode; 
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

        // 1. Fetch Sales
        List<Sale> allSales;
        try {
            if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                
                // 🚀 FIX: Use the JOIN FETCH query to ensure Items (and Cost Price) are loaded
                allSales = saleRepo.findSalesWithItems(start, end); 
            } else {
                // Default: Fetch all (Be careful with large data!)
                allSales = saleRepo.findAll(Sort.by(Sort.Direction.DESC, "date"));
            }
        } catch (Exception e) {
            System.err.println("Date Error: " + e.getMessage());
            allSales = new ArrayList<>();
        }

        // 2. Total Revenue
        BigDecimal totalRevenue = allSales.stream()
                .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Low Stock Count
        List<Product> products = productRepo.findAll(); // Fetch once for efficiency
        long lowStockCount = products.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= 5).count();

        // 🚀 NEW: Calculate Profit & Margin
        // Create a quick lookup map for Cost Prices: ID -> CostPrice
        Map<Long, Double> productCostMap = products.stream()
            .collect(Collectors.toMap(Product::getId, p -> p.getCostPrice() != null ? p.getCostPrice() : 0.0));

        BigDecimal totalCost = BigDecimal.ZERO;

        for (Sale sale : allSales) {
            if (sale.getItems() == null) continue;
            for (SaleItem item : sale.getItems()) {
                // Get Cost Price from map (default to 0 if missing)
                Double cp = productCostMap.getOrDefault(item.getProductId(), 0.0);
                BigDecimal itemTotalCost = BigDecimal.valueOf(cp).multiply(BigDecimal.valueOf(item.getQuantity()));
                totalCost = totalCost.add(itemTotalCost);
            }
        }

        BigDecimal totalProfit = totalRevenue.subtract(totalCost);
        BigDecimal profitMargin = (totalRevenue.compareTo(BigDecimal.ZERO) > 0)
                ? totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // 4. Best Sellers (Top Products)
        Map<String, Integer> productSales = allSales.stream()
                .flatMap(s -> s.getItems().stream())
                .filter(item -> item.getProductName() != null)
                .collect(Collectors.groupingBy(SaleItem::getProductName, Collectors.summingInt(SaleItem::getQuantity)));

        Map<String, Integer> topProducts = productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // 5. Category Revenue
        Map<Long, String> productCategories = products.stream()
            .collect(Collectors.toMap(Product::getId, 
                    p -> p.getCategory() != null ? p.getCategory() : "Uncategorized",
                    (existing, replacement) -> existing));
        
        Map<String, Double> categoryRevenue = new HashMap<>();
        for (Sale sale : allSales) {
            if (sale.getItems() == null) continue;
            for (SaleItem item : sale.getItems()) {
                double price = (item.getPrice() != null) ? item.getPrice().doubleValue() : 0.0;
                int qty = item.getQuantity();
                
                String cat = productCategories.getOrDefault(item.getProductId(), "Uncategorized");
                categoryRevenue.put(cat, categoryRevenue.getOrDefault(cat, 0.0) + (price * qty));
            }
        }

        // 6. Staff Leaderboard
        Map<String, BigDecimal> staffPerformance = allSales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCashierName() != null ? s.getCashierName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, 
                                s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO, 
                                BigDecimal::add)
                ));

        // 7. Recent Sales
        List<Sale> recentSales = allSales.stream()
                .sorted(Comparator.comparing(Sale::getDate).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // 8. VIP Customers
        List<Customer> topCustomers = new ArrayList<>();
        try { topCustomers = customerRepo.findTop20ByOrderByPointsDesc(); } catch (Exception e) {}

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", allSales.size());
        stats.put("lowStockCount", lowStockCount);
        stats.put("topProducts", topProducts);
        stats.put("categoryRevenue", categoryRevenue);
        stats.put("staffPerformance", staffPerformance);
        stats.put("recentSales", recentSales);
        stats.put("topCustomers", topCustomers);
        
        // 🚀 Add Profit Stats
        stats.put("totalProfit", totalProfit);
        stats.put("profitMargin", profitMargin);
        
        return stats;
    }

    @GetMapping("/stats/ai")
    public Map<String, Object> getAiStats() {
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