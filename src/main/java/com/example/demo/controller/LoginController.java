package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login"; // Looks for src/main/resources/templates/login.html
    }
    
    @GetMapping("/")
    public String home() {
        return "index"; // Looks for src/main/resources/templates/index.html
    }
}