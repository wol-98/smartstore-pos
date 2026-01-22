package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; 
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth
                // ✅ 1. Static Resources (Public)
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll() 
                
                // ✅ 2. Login & Error Pages (Public)
                .requestMatchers("/login", "/error").permitAll()                
                
                // 🚀 3. SWAGGER UI WHITELIST (NEW ADDITION)
                // Allows anyone to view API docs without logging in
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // 🧠 4. Enterprise Security: Protect Dashboard
                .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                
                // 🔒 5. Everything else requires login
                .anyRequest().authenticated()                                    
            )
            .formLogin(form -> form
                .loginPage("/login") 
                .defaultSuccessUrl("/", true) 
                .permitAll()
            )
            .logout(logout -> logout.permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}