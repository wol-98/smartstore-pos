package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime date = LocalDateTime.now();
    private BigDecimal totalAmount;
    private String status;       // Paid, Pending
    private String paymentMethod; // Cash, Card, UPI
    
    // Track which cashier made the sale
    private String cashierName;

    // 🚨 NEW: Store the Customer's Phone (Crucial for PDF)
    private String customerPhone; 

    @OneToMany(cascade = CascadeType.ALL)
    private List<SaleItem> items;

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }

    // 🚨 NEW Getters & Setters for Phone
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
}