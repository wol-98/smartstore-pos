package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("🔍 Login Attempt for username: " + username); // Debug Log 1

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("❌ User Not Found in DB: " + username); // Debug Log 2
                    return new UsernameNotFoundException("User not found");
                });

        System.out.println("✅ User Found: " + user.getUsername()); // Debug Log 3
        System.out.println("🔐 Hash from DB: " + user.getPassword()); // Debug Log 4

        // Convert your "User" entity into a Spring Security "UserDetails" object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}