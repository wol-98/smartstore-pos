package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Sale> sales = saleRepo.findAll();
        List<Product> products = productRepo.findAll();

        // Filter by Date if provided
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            sales = sales.stream()
                    .filter(s -> s.getDate().isAfter(start) && s.getDate().isBefore(end))
                    .toList();
        }

        BigDecimal totalRevenue = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long lowStockCount = products.stream()
                .filter(p -> p.getStock() < 5)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", sales.size());
        stats.put("lowStockCount", lowStockCount);

        return stats;
    }
}
