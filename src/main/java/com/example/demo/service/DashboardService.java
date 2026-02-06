package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private CustomerRepository customerRepo; 

    // 🎯 TARGET: ₹20,000/day
    private static final double DAILY_TARGET = 20000.0;

    public Map<String, Object> getDashboardStats(LocalDate start, LocalDate end) {
        // 1. Precise Date Range
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);

        // 2. Fetch Sales Data
        List<Sale> sales = saleRepo.findSalesWithItems(startDt, endDt);
        long lifetimeOrders = saleRepo.count();

        // --- 🔧 FIX: Correct Stock Logic (Check WHOLE Inventory) ---
        // This fixes the issue where alerts were 0 because it only checked sold items.
        List<Product> allProducts = productRepo.findAll();
        long lowStockCount = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= 5)
                .count();
        // -----------------------------------------------------------

        // 3. Initialize Counters
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int periodOrders = sales.size(); 

        // 📊 Maps for Charts
        Map<String, Integer> productSales = new HashMap<>();
        Map<String, BigDecimal> categoryRevenue = new HashMap<>();
        Map<String, BigDecimal> staffPerformance = new HashMap<>();
        Map<String, BigDecimal> paymentStats = new HashMap<>();
        Map<Integer, Integer> hourlyTraffic = new HashMap<>(); 

        // Initialize Hourly Map
        for(int i=9; i<=21; i++) hourlyTraffic.put(i, 0); 

        // 4. MAIN ANALYTICS LOOP
        for (Sale sale : sales) {
            BigDecimal amount = (sale.getTotalAmount() != null) ? sale.getTotalAmount() : BigDecimal.ZERO;
            totalRevenue = totalRevenue.add(amount);

            // A. Payment Stats
            String pMethod = (sale.getPaymentMethod() != null) ? sale.getPaymentMethod().toUpperCase() : "CASH";
            paymentStats.merge(pMethod, amount, BigDecimal::add);

            // B. Staff Performance
            String cashier = (sale.getCashierName() != null) ? sale.getCashierName() : "Unknown";
            staffPerformance.merge(cashier, amount, BigDecimal::add);

            // C. Hourly Traffic
            if (sale.getDate() != null) {
                int hour = sale.getDate().getHour();
                if(hour >= 9 && hour <= 21) {
                    hourlyTraffic.put(hour, hourlyTraffic.getOrDefault(hour, 0) + 1);
                }
            }

            // D. Item Analysis
            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    // Safe Retrieval
                    if (item.getProductId() == null) continue;

                    Product p = productRepo.findById(item.getProductId()).orElse(null);
                    
                    if (p != null) {
                        // Profit Calculation
                        if (p.getBuyingPrice() != null) {
                            BigDecimal cost = p.getBuyingPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                            totalCost = totalCost.add(cost);
                        }
                        
                        // Note: Removed the old "Stock Alert" check here as it was incorrect.
                        
                        // Category Revenue
                        if (p.getCategory() != null) {
                            BigDecimal itemPrice = (item.getPrice() != null) ? item.getPrice() : BigDecimal.ZERO;
                            BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                            categoryRevenue.merge(p.getCategory(), lineTotal, BigDecimal::add);
                        }
                        
                        // Best Sellers Count
                        String pName = (item.getProductName() != null) ? item.getProductName() : "Unknown";
                        productSales.put(pName, productSales.getOrDefault(pName, 0) + item.getQuantity());
                    }
                }
            }
        }

        // 5. Final Calculations
        BigDecimal totalProfit = totalRevenue.subtract(totalCost);
        BigDecimal profitMargin = (totalRevenue.compareTo(BigDecimal.ZERO) > 0)
                ? totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        double targetPercent = (totalRevenue.doubleValue() / DAILY_TARGET) * 100;
        if(targetPercent > 100) targetPercent = 100;

        // 6. Sort & Package Response
        
        // Top 5 Products
        Map<String, Integer> topProducts = productSales.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Descending
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // VIP Customers
        List<Customer> topCustomers = new ArrayList<>();
        try {
            topCustomers = customerRepo.findTop20ByOrderByPointsDesc();
        } catch (Exception e) {
            System.err.println("Warning: Could not fetch VIP customers");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalRevenue", totalRevenue);
        response.put("totalOrders", periodOrders); 
        response.put("lifetimeOrders", lifetimeOrders); 
        
        // ✅ UPDATED: Use the correct inventory-wide count
        response.put("lowStockCount", lowStockCount);
        
        // Profit Stats
        response.put("totalProfit", totalProfit);
        response.put("profitMargin", profitMargin);
        
        // Charts Data
        response.put("topProducts", topProducts);
        response.put("categoryRevenue", categoryRevenue);
        response.put("staffPerformance", staffPerformance);
        response.put("paymentStats", paymentStats);
        response.put("hourlyTraffic", hourlyTraffic); 
        
        // Targets & Lists
        response.put("dailyTarget", DAILY_TARGET);
        response.put("targetPercent", targetPercent);
        response.put("topCustomers", topCustomers);
        
        // Recent Sales List
        response.put("recentSales", sales.stream()
                .sorted(Comparator.comparing(Sale::getDate).reversed())
                .limit(10)
                .collect(Collectors.toList()));

        return response;
    }
}