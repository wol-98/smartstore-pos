package com.example.demo.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/api/me")
    public Map<String, String> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // SAFETY CHECK: If auth is null, return "Guest" instead of crashing
        String name = (auth != null) ? auth.getName() : "Guest";
        
        return Collections.singletonMap("username", name);
    }
}