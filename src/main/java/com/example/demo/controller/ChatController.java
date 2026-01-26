package com.example.demo.controller;

import com.example.demo.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping
    public Map<String, String> ask(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        String answer = chatbotService.processQuery(question);
        return Map.of("answer", answer);
    }
}