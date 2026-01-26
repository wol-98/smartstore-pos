package com.example.demo.controller;

import com.example.demo.model.Customer;
import com.example.demo.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/store/auth")
public class CustomerAuthController {

    @Autowired
    private CustomerRepository customerRepo;

    // 1. REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Customer cust) {
        if (customerRepo.findByPhone(cust.getPhone()) != null) {
            return ResponseEntity.badRequest().body("Phone number already registered!");
        }
        // In a real app, encrypt this password!
        cust.setPoints(0);
        customerRepo.save(cust);
        return ResponseEntity.ok("Registration successful!");
    }

    // 2. LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> data) {
        String phone = data.get("phone");
        String password = data.get("password");

        Customer cust = customerRepo.findByPhone(phone);
        
        if (cust != null && cust.getPassword().equals(password)) {
            // Return Customer Details (Simple Session)
            return ResponseEntity.ok(Map.of(
                "id", cust.getId(),
                "name", cust.getName(),
                "phone", cust.getPhone(),
                "address", cust.getAddress() != null ? cust.getAddress() : ""
            ));
        }
        return ResponseEntity.status(401).body("Invalid Credentials");
    }
}