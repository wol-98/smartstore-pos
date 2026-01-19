package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            createOrUpdateUser(repo, encoder, "Raphael", "1998wol!", "ADMIN");
            createOrUpdateUser(repo, encoder, "Matty", "Ngor@1998!", "CASHIER");
            createOrUpdateUser(repo, encoder, "Ian", "Ngor@1998!", "CASHIER");
        };
    }

    private void createOrUpdateUser(UserRepository repo, PasswordEncoder encoder, String name, String rawPassword, String role) {
        User u = repo.findByUsername(name).orElse(new User());
        
        u.setUsername(name);
        u.setRole(role);

        // 🚨 NUCLEAR FIX: NO IF STATEMENT.
        // We overwrite the password every single time to guarantee it is correct.
        u.setPassword(encoder.encode(rawPassword));
        
        repo.save(u);
        System.out.println("✅ User Forced & Updated: " + name);
    }
}