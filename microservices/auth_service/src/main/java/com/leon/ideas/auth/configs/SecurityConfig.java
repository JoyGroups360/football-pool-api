package com.leon.ideas.auth.configs;

import com.leon.ideas.auth.security.JwtAuthenticationEntryPoint;
import com.leon.ideas.auth.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // Endpoints públicos (no requieren autenticación)
                        .requestMatchers(HttpMethod.POST, "/football-pool/v1/api/auth").permitAll() // Login
                        .requestMatchers(HttpMethod.GET, "/football-pool/v1/api/auth").permitAll() // Get all users
                        .requestMatchers(HttpMethod.POST, "/football-pool/v1/api/auth/forgot-password").permitAll() // Forgot password
                        .requestMatchers(HttpMethod.POST, "/football-pool/v1/api/auth/validate-token").permitAll() // Validar token
                        .requestMatchers(HttpMethod.POST, "/football-pool/v1/api/auth/refresh-token").permitAll() // Refresh token
                        .requestMatchers(HttpMethod.POST, "/football-pool/v1/api/auth/create").permitAll() // Crear usuario
                        .requestMatchers(HttpMethod.POST, "/football-pool/v1/api/auth/reset-password").permitAll() // Reset password
                        .requestMatchers(HttpMethod.POST, "/football-pool/v1/api/auth/social").permitAll() // Social auth (Facebook/Google)
                        
                        // Endpoints protegidos (requieren autenticación JWT)
                        .requestMatchers(HttpMethod.PATCH, "/football-pool/v1/api/auth/id").authenticated() // PATCH
                        .requestMatchers(HttpMethod.DELETE, "/football-pool/v1/api/auth/id").authenticated() // DELETE
                        .requestMatchers(HttpMethod.PUT, "/football-pool/v1/api/auth/complete-profile").authenticated() // Complete profile
                        .anyRequest().permitAll() // Por defecto permitir todo
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                
        return http.build();
    }
}