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
                // ✅ 1. Allow Static Resources (CSS, JS, Images, WebJars for Icons)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                
                // ✅ 2. Allow Public Access to Login & Error
                .requestMatchers("/login", "/error").permitAll()
                
                // 🚀 3. Swagger UI Whitelist
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
            // 🚀 INTEGRATE CUSTOM LOGIN PAGE HERE
            .formLogin(form -> form
                .loginPage("/login") // Matches the @Controller mapping
                .defaultSuccessUrl("/", true) // Redirect to Dashboard on success
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout") // Redirect back to login on logout
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}