package com.example.demo.model;

public class SaleItemRequest {
    
    private Integer productId;
    private Integer quantity;

    // --- MANUAL GETTERS AND SETTERS ---
    
    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}