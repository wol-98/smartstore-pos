package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; 
    private String category; 
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private LocalDateTime submittedAt;
    
    // 🚀 NEW FIELDS FOR WORKFLOW
    private String status; // "PENDING", "RESOLVED"
    private Long linkedProductId; // For Stock Requests

    public Feedback() {
        this.submittedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLinkedProductId() { return linkedProductId; }
    public void setLinkedProductId(Long linkedProductId) { this.linkedProductId = linkedProductId; }
}