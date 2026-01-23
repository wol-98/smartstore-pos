package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 👈 FIXED: Changed Integer to Long

    private String phone;
    private String name;
    private Integer points;

    // Getters and Setters
    public Long getId() { return id; } // 👈 Updated return type
    public void setId(Long id) { this.id = id; } // 👈 Updated parameter type
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
}