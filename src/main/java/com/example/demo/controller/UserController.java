package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- 1. EXISTING ENDPOINT: Who am I? ---
    @GetMapping("/me")
    public Map<String, String> getCurrentUser() {
        Map<String, String> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            // DEMO MODE: If not logged in, pretend to be Admin
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                response.put("username", "Demo Admin");
                response.put("role", "ADMIN");
                return response;
            }

            String username = auth.getName();
            Optional<User> userOpt = userRepo.findByUsername(username);

            if (userOpt.isPresent()) {
                response.put("username", userOpt.get().getUsername());
                response.put("role", userOpt.get().getRole());
            } else {
                response.put("username", username);
                response.put("role", "STAFF");
            }
            
        } catch (Exception e) {
            response.put("username", "Guest");
            response.put("role", "ADMIN");
        }

        return response;
    }

    // --- 2. NEW ENDPOINTS: User Management (For Admin Panel) ---

    // Get all users for the table
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // Create a new Cashier/Admin
    @PostMapping("/users")
    public User createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String rawPassword = body.get("password");
        String role = body.get("role"); // "ADMIN" or "STAFF"

        if (userRepo.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists!");
        }

        User u = new User();
        u.setUsername(username);
        // 🔒 CRITICAL: Hash the password so it works with Spring Security
        u.setPassword(passwordEncoder.encode(rawPassword)); 
        u.setRole(role);
        
        return userRepo.save(u);
    }

    // Delete a user
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userRepo.deleteById(id);
    }
}