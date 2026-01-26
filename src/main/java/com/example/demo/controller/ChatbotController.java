package com.example.demo.controller;

import com.example.demo.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*; // 👈 Make sure this import is here!

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController { // 👈 Ensure class name matches file name

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        // Ensure service exists and process the question
        String answer = (chatbotService != null) ? chatbotService.processQuery(question) : "Service Unavailable";
        return Map.of("answer", answer);
    }
}