package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
            .csrf(csrf -> csrf.disable()) // Disable CSRF for easier API testing
            .authorizeHttpRequests(auth -> auth
                // ✅ 1. Allow Static Resources (CSS, JS, Images)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                
                // ✅ 2. Allow Public Pages (Login, Kiosk, Storefront)
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers("/store.html", "/kiosk.html").permitAll()

                // ✅ 3. CUSTOMER APIs (Safe to be Public)
                .requestMatchers("/api/store/auth/**").permitAll()       // Customer Signup/Login
                .requestMatchers("/api/recommendations/**").permitAll()  // AI Upsell Engine
                .requestMatchers("/api/feedback/**").permitAll()         // Sending Feedback

                // ⚠️ 4. HYBRID APIs (Public READ, Admin WRITE)
                // Anyone can VIEW products (to shop), but only Admin can EDIT them
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()   
                .requestMatchers(HttpMethod.POST, "/api/sales/**").permitAll()     // Anyone can create a sale (Checkout)

                // 🔒 5. ADMIN ONLY ZONES (The "Vault")
                .requestMatchers("/api/dashboard/**").hasRole("ADMIN")             // View Profits/Stats
                .requestMatchers("/api/suppliers/**").hasRole("ADMIN")             // Manage Vendors
                
                // Dangerous Product Operations (Write/Edit/Delete)
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN") 
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")  
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN") 

                // 🚀 6. Swagger UI (Keep public for documentation/testing)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // 🔒 7. Everything else requires a login
                .anyRequest().authenticated()
            )
            // ✅ Standard Login Form
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            // ✅ Logout Config
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}