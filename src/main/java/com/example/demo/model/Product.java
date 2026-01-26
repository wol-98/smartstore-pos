package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 
    
    private String name;
    
    // 5 Ps Fields 
    private String brand;
    private String category;
    private Double price;
    private Double discount;
    private Double costPrice;
    private Long supplierId;

    // Inventory
    private Integer stock;      
    
    @Column(name = "min_stock") 
    private Integer minStock;

    // 🚀 NEW FLAG: Tracks if we already sent an email for this drop
    private Boolean isAlertSent = false; 

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Double getCostPrice() { return costPrice; }
    public void setCostPrice(Double costPrice) { this.costPrice = costPrice; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }

    // 🚀 Getter/Setter for the new flag
    public Boolean getIsAlertSent() { return isAlertSent != null ? isAlertSent : false; }
    public void setIsAlertSent(Boolean isAlertSent) { this.isAlertSent = isAlertSent; }
}