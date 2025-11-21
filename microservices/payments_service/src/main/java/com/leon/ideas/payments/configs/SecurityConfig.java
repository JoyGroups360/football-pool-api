package com.leon.ideas.payments.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // Health check endpoint (public)
                        .requestMatchers(HttpMethod.GET, "/football-pool/v1/api/payments/health").permitAll()
                        
                        // Webhook endpoint for Stripe (public, but should be secured with webhook secret)
                        // TODO: Add webhook signature verification
                        // .requestMatchers(HttpMethod.POST, "/football-pool/v1/api/payments/webhook").permitAll()
                        
                        // All other endpoints require authentication (handled in controller)
                        .anyRequest().permitAll() // Controller will handle authentication
                );
                
        return http.build();
    }
}

