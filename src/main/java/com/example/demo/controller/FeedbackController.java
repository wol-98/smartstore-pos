package com.example.demo.controller;

import com.example.demo.model.Feedback;
import com.example.demo.model.Product;
import com.example.demo.repository.FeedbackRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication; 
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired private FeedbackRepository feedbackRepo;
    @Autowired private ProductRepository productRepo; 
    @Autowired private JavaMailSender mailSender; 

    // 1. SUBMIT FEEDBACK (With Auto-Email for Bugs)
    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> payload, Authentication auth) {
        try {
            Feedback f = new Feedback();
            f.setMessage((String) payload.get("message"));
            f.setCategory((String) payload.get("category"));
            f.setUsername(auth != null ? auth.getName() : "Anonymous");
            
            // Link Product if provided (For Stock Requests)
            if (payload.containsKey("productId") && payload.get("productId") != null) {
                String pidStr = payload.get("productId").toString();
                if(!pidStr.isEmpty()) f.setLinkedProductId(Long.parseLong(pidStr));
            }

            feedbackRepo.save(f);

            // 🚀 EMAIL ALERT: Only for "Bug" category
            if ("Bug".equalsIgnoreCase(f.getCategory())) {
                sendBugAlert(f);
            }

            return ResponseEntity.ok("Feedback received!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving feedback");
        }
    }

    // 2. GET ALL (For Admin Inbox)
    @GetMapping
    public List<Feedback> getAllFeedback() {
        return feedbackRepo.findAll();
    }

    // 3. MARK RESOLVED
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveFeedback(@PathVariable Long id) {
        return feedbackRepo.findById(id).map(f -> {
            f.setStatus("RESOLVED");
            feedbackRepo.save(f);
            return ResponseEntity.ok("Resolved");
        }).orElse(ResponseEntity.notFound().build());
    }

    // 4. APPROVE STOCK REQUEST (Auto-Add Stock)
    @PutMapping("/{id}/approve-stock")
    public ResponseEntity<?> approveStock(@PathVariable Long id) {
        Feedback f = feedbackRepo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        
        if (f.getLinkedProductId() != null) {
            // 📦 Adds 10 units automatically
            Product p = productRepo.findById(f.getLinkedProductId()).orElseThrow();
            p.setStock(p.getStock() + 10); 
            productRepo.save(p);
            
            f.setStatus("APPROVED (Stock Added)");
            feedbackRepo.save(f);
            return ResponseEntity.ok("Stock added and request resolved");
        }
        return ResponseEntity.badRequest().body("No product linked");
    }

    // --- Async Email Helper ---
    private void sendBugAlert(Feedback f) {
        new Thread(() -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo("raphaelwol20@gmail.com"); 
                msg.setSubject("🚨 BUG REPORT: " + f.getUsername());
                msg.setText("User: " + f.getUsername() + "\nMessage: " + f.getMessage());
                mailSender.send(msg);
            } catch (Exception e) {
                System.err.println("Failed to send alert: " + e.getMessage());
            }
        }).start();
    }
}