package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String phone;
    private String name;
    private Integer points;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
}