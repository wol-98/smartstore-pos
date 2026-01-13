package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // UPDATED PASSWORD FOR ALL ADMINS
        String securePassword = "Ngor@1998!";

        UserDetails raphael = User.withDefaultPasswordEncoder()
                .username("Raphael")
                .password(securePassword)
                .roles("ADMIN")
                .build();

        UserDetails matty = User.withDefaultPasswordEncoder()
                .username("Matty")
                .password(securePassword)
                .roles("ADMIN")
                .build();

        UserDetails ian = User.withDefaultPasswordEncoder()
                .username("Ian")
                .password(securePassword)
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(raphael, matty, ian);
    }
}