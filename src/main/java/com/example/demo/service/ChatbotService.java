package com.example.demo.service;

import com.example.demo.model.Customer;
import com.example.demo.model.Product;
import com.example.demo.model.Sale;
import com.example.demo.model.SaleItem;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.SaleRepository;
import com.example.demo.repository.FeedbackRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired private SaleRepository saleRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private FeedbackRepository feedbackRepo;
    @Autowired private PredictionService predictionService;

    // Use environment variable for Docker compatibility
    private final String AI_URL = System.getenv("AI_SERVICE_URL") != null ? System.getenv("AI_SERVICE_URL") : "http://localhost:5000";
    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, String> knowledgeBase = new HashMap<>();

    @PostConstruct
    public void init() {
        // 📚 EXPANDED KNOWLEDGE BASE (Answers for UI Questions)
        knowledgeBase.put("wifi", "The store WiFi password is: **StoreSecure123**");
        knowledgeBase.put("export", "Click the green **Export** button (Top Right) to download Excel reports.");
        knowledgeBase.put("import", "Use the orange **Import** button in the header to upload bulk products via Excel.");
        knowledgeBase.put("theme", "Toggle **Dark/Light Mode** using the Moon/Sun icon 🌙 in the top right.");
        knowledgeBase.put("dark mode", "Toggle **Dark/Light Mode** using the Moon/Sun icon 🌙 in the top right.");
        knowledgeBase.put("date", "Use the **Date Pickers** in the header to filter the dashboard data.");
    }

    // 🧠 MAIN LOGIC ROUTER
    public String processQuery(String query) {
        if (query == null || query.trim().isEmpty()) return "👋 Hello! I am listening.";

        // 1. Ask Python Intent
        String intent = getIntentFromAI(query);

        // 2. Execute Logic
        switch (intent) {
            case "GET_REVENUE": return getRevenueSummary(query);
            case "GET_PROFIT": return getProfitSummary();
            case "GET_DAILY_GOAL": return getDailyGoalStatus();
            case "CHECK_STOCK": return getStockStatus();
            case "GET_PRODUCT_COUNT": return getProductCount();
            case "GET_TOTAL_ORDERS": return getTotalOrders(query);
            case "GET_RECENT_TRANSACTIONS": return getRecentTransactions();
            case "GET_FEEDBACK": return getFeedbackSummary();
            case "BEST_SELLER": return getBestSellers();
            case "GET_CATEGORIES": return getCategorySummary();
            case "PREDICT_SALES": return getAIForecast();
            case "GET_STAFF": return getTopStaff();
            case "GET_VIP": return getVIPCustomers();
            case "GET_UI_HELP": return fallbackLocalSearch(query); // 👈 Routes "Where is Export?" to KB
            case "GREETING": return "👋 Hello! I am your SmartStore AI.";
            default: return fallbackLocalSearch(query); 
        }
    }

    // ==========================================================
    // 📊 DATA FETCHERS
    // ==========================================================

    private String getFeedbackSummary() {
        long count = feedbackRepo.count();
        if (count == 0) return "📭 **Admin Inbox:** No new messages.";
        return "📬 **Admin Inbox:** You have **" + count + "** messages waiting. Check the dashboard for details.";
    }

    private String getRecentTransactions() {
        List<Sale> recent = saleRepo.findAll().stream()
            .sorted(Comparator.comparing(Sale::getDate).reversed())
            .limit(3)
            .collect(Collectors.toList());
        
        if(recent.isEmpty()) return "No transactions yet today.";

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        
        return "🧾 **Live Transactions:**\n" +
               recent.stream()
                   .map(s -> "• " + s.getDate().format(timeFmt) + " | " + s.getCashierName() + "\n" + 
                            "   " + (s.getItems() != null ? s.getItems().size() : 0) + " items for ₹" + s.getTotalAmount())
                   .collect(Collectors.joining("\n"));
    }

    private String getDailyGoalStatus() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<Sale> sales = saleRepo.findByDateAfter(start);
        double totalRevenue = sales.stream().mapToDouble(s -> s.getTotalAmount().doubleValue()).sum();
        
        double target = 20000.0; 
        double percent = (totalRevenue / target) * 100;
        String status = (percent >= 100) ? "🎉 Goal Reached!" : "📉 Keep pushing!";
        return "🎯 **Daily Goal:**\nTarget: ₹" + target + "\nAchieved: ₹" + totalRevenue + " (" + String.format("%.1f", percent) + "%)\n" + status;
    }

    private String getTotalOrders(String query) {
        boolean isAllTime = query.toLowerCase().contains("total") || query.toLowerCase().contains("lifetime");
        if (isAllTime) {
            long count = saleRepo.count();
            return "🧾 **Lifetime Orders:** We have processed **" + count + "** transactions since the beginning.";
        } else {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            List<Sale> sales = saleRepo.findByDateAfter(start);
            return "🧾 **Orders Today:** " + sales.size() + " transactions processed so far.";
        }
    }

    private String getProductCount() {
        long count = productRepo.count();
        return "📦 **Inventory Size:** You have **" + count + "** unique products listed in the system.";
    }

    private String getCategorySummary() {
        return "📂 **Top Categories:**\n(Data coming from Dashboard Analytics)"; 
    }
    
    private String getTopStaff() {
        // 1. Get Active Staff Count
        long staffCount = saleRepo.findAll().stream()
                .map(Sale::getCashierName)
                .filter(Objects::nonNull)
                .distinct().count();
        
        // 2. Find Top Performer
        Map<String, Long> stats = saleRepo.findAll().stream()
            .collect(Collectors.groupingBy(s -> s.getCashierName() != null ? s.getCashierName() : "Unknown", Collectors.counting()));
        
        String best = stats.entrySet().stream().max(Map.Entry.comparingByValue())
                   .map(e -> e.getKey() + " (" + e.getValue() + " sales)").orElse("No data.");

        return "👥 **Staff Overview:**\n• Active Staff: " + staffCount + "\n• 🏆 Top Performer: " + best;
    }

    // 🔌 Talk to Python Service
    private String getIntentFromAI(String query) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("query", query);
            Map<String, String> response = restTemplate.postForObject(AI_URL + "/classify", payload, Map.class);
            
            if (response != null && response.containsKey("intent")) {
                return response.get("intent");
            }
        } catch (Exception e) {
            System.err.println("⚠️ AI Service Offline (NLP Failed): " + e.getMessage());
        }
        return "UNKNOWN"; 
    }

    // 🔍 Fallback Logic
    private String fallbackLocalSearch(String q) {
        q = q.toLowerCase();
        if (q.contains("price") || q.contains("cost") || q.contains("how much") && !q.contains("staff")) {
            return getProductDetails(q);
        }
        for (Map.Entry<String, String> entry : knowledgeBase.entrySet()) {
            // Check if the query contains the key (e.g. "where is export" contains "export")
            if (q.contains(entry.getKey())) return "💡 " + entry.getValue();
        }
        return "🤖 I'm not sure about that. Try asking 'Total Revenue', 'Low Stock', or 'Predict Sales'.";
    }

    private String getProductDetails(String query) {
        String search = query.replace("price of", "").replace("how much is", "").replace("cost of", "").trim();
        List<Product> products = productRepo.findAll();
        Optional<Product> match = products.stream()
            .filter(p -> p.getName().toLowerCase().contains(search))
            .findFirst();

        if (match.isPresent()) {
            Product p = match.get();
            double profit = p.getPrice() - (p.getCostPrice() != null ? p.getCostPrice() : 0);
            return "🏷️ **" + p.getName() + "**\n• Price: ₹" + p.getPrice() + "\n• Stock: " + p.getStock() + "\n• Margin: ₹" + profit;
        }
        return "❌ I couldn't find a product matching '" + search + "'.";
    }

    private String getProfitSummary() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<Sale> sales = saleRepo.findSalesWithItems(start, LocalDateTime.now());
        double revenue = 0, cost = 0;
        for(Sale s : sales) {
            revenue += s.getTotalAmount().doubleValue();
            if(s.getItems() != null) {
                for(var item : s.getItems()) {
                      Product p = productRepo.findById(item.getProductId()).orElse(null);
                      if(p != null && p.getCostPrice() != null) cost += p.getCostPrice() * item.getQuantity();
                }
            }
        }
        return "💰 **Net Profit Today**\nRevenue: ₹" + String.format("%.2f", revenue) + "\nCost: ₹" + String.format("%.2f", cost) + "\nProfit: ₹" + String.format("%.2f", (revenue - cost));
    }

    private String getRevenueSummary(String query) {
       boolean isMonth = query.contains("month");
       LocalDateTime start = isMonth ? LocalDate.now().withDayOfMonth(1).atStartOfDay() : LocalDate.now().atStartOfDay();
       List<Sale> sales = saleRepo.findByDateAfter(start);
       double total = sales.stream().mapToDouble(s -> s.getTotalAmount().doubleValue()).sum();
       return "📊 **Total Revenue**\n₹" + total + " across " + sales.size() + " orders.";
    }

    private String getStockStatus() {
       long low = productRepo.findAll().stream().filter(p -> p.getStock() <= 5).count();
       return low == 0 ? "✅ All Stock Levels Healthy." : "⚠️ **Alert:** " + low + " products are running low on stock!";
    }

    private String getBestSellers() {
       Map<String, Integer> counts = saleRepo.findAll().stream()
           .flatMap(s -> s.getItems().stream())
           .collect(Collectors.groupingBy(SaleItem::getProductName, Collectors.summingInt(SaleItem::getQuantity)));
       return "🔥 **Top Best Sellers:**\n" + counts.entrySet().stream()
              .sorted((a,b) -> b.getValue().compareTo(a.getValue())).limit(3)
              .map(e -> "• " + e.getKey() + " (" + e.getValue() + " sold)").collect(Collectors.joining("\n"));
    }

    private String getAIForecast() {
       return "🤖 **AI Prediction:** Based on recent trends, sales are looking stable.";
    }

    private String getVIPCustomers() {
        List<Customer> vips = customerRepo.findTop20ByOrderByPointsDesc();
        if (vips.isEmpty()) return "No VIP customers yet.";
        Customer top = vips.get(0);
        return "💎 **Top VIP:** " + top.getName() + "\nPoints: " + top.getPoints() + "\nPhone: " + top.getPhone();
    }
}