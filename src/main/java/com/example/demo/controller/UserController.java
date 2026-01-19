package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/me")
    public Map<String, String> getCurrentUser() {
        // 1. Get the logged-in username
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // 2. Find the user in the database to get their Role
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Send BOTH Username AND Role to the frontend
        Map<String, String> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("role", user.getRole()); // 🚨 This logic was missing before!

        return response;
    }
}