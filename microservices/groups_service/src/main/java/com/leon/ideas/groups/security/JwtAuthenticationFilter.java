package com.leon.ideas.groups.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String requestPath = request.getRequestURI();
        
        System.out.println("🔐 JWT Filter - Processing request: " + requestPath);
        System.out.println("🔐 JWT Filter - Authorization header present: " + (authHeader != null));
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("⚠️ JWT Filter - No Authorization header or invalid format");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            System.out.println("🔐 JWT Filter - Token extracted (length: " + jwt.length() + ")");
            
            // Validate token first
            if (!jwtService.isTokenValid(jwt)) {
                System.err.println("❌ JWT Filter - Token is invalid or expired");
                filterChain.doFilter(request, response);
                return;
            }
            
            final String userEmail = jwtService.extractEmail(jwt);
            final String userId = jwtService.extractUserId(jwt);
            
            System.out.println("🔐 JWT Filter - Token valid. Email: " + userEmail + ", UserId: " + userId);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail,
                        null,
                        new ArrayList<>()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Store userId in request attribute for easy access
                request.setAttribute("userId", userId);
                request.setAttribute("userEmail", userEmail);
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("✅ JWT Filter - Authentication set successfully");
            } else {
                System.out.println("⚠️ JWT Filter - Email is null or authentication already exists");
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.err.println("❌ JWT Filter - Token expired: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            System.err.println("❌ JWT Filter - Invalid signature (JWT secret mismatch?): " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            System.err.println("❌ JWT Filter - Malformed token: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        } catch (Exception e) {
            System.err.println("❌ JWT Filter - Unexpected error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}


