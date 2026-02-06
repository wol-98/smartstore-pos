package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Entity
@Table(name = "product") 
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 
    
    private String name;
    
    // 5 Ps Fields 
    private String brand;
    private String category;

    // Maps to 'selling_price' column in DB
    @Column(name = "selling_price", nullable = false, precision = 10, scale = 2)
    @Min(value = 0, message = "Selling price must be positive")
    private BigDecimal sellingPrice;

    // Maps to 'buying_price' column in DB
    @Column(name = "buying_price", nullable = false, precision = 10, scale = 2)
    @Min(value = 0, message = "Buying price must be positive")
    private BigDecimal buyingPrice;

    private Double discount;
    private Long supplierId;

    // Inventory
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;      
    
    @Column(name = "min_stock") 
    private Integer minStock;

    // Tracks if we already sent an email for this drop
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

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public BigDecimal getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(BigDecimal buyingPrice) { this.buyingPrice = buyingPrice; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }

    public Boolean getIsAlertSent() { return isAlertSent != null ? isAlertSent : false; }
    public void setIsAlertSent(Boolean isAlertSent) { this.isAlertSent = isAlertSent; }
}