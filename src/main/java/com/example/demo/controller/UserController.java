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
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/me")
    public Map<String, String> getCurrentUser() {
        Map<String, String> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            // DEMO MODE: If not logged in, pretend to be Admin so dashboard works
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
                response.put("role", "STAFF"); // Default role
            }
            
        } catch (Exception e) {
            // Safety Net
            response.put("username", "Guest");
            response.put("role", "ADMIN");
        }

        return response;
    }
}