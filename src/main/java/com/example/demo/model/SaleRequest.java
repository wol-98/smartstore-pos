package com.example.demo.model;

import java.util.List;

public class SaleRequest {
    private String cashierName; 
    private List<SaleItemRequest> items;

    // --- MANUAL GETTERS AND SETTERS ---

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public List<SaleItemRequest> getItems() { return items; }
    public void setItems(List<SaleItemRequest> items) { this.items = items; }
}