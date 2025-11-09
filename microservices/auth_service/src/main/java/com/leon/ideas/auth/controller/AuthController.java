package com.leon.ideas.auth.controller;

import com.leon.ideas.auth.service.AuthService;
import com.leon.ideas.auth.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import org.bson.Document;
@RestController
@RequestMapping("/football-pool/v1/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtService jwtService;

    @GetMapping
    public ResponseEntity<Document> getUsers() {
        try {
            List<Document> users = authService.getAllUsers();
            Document response = new Document("users", users);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error in getUsers: " + e.getMessage());
            e.printStackTrace();
            Document errorResponse = new Document("error", "Failed to retrieve users: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Document> userExists(@PathVariable String userId) {
        boolean exists = authService.userExistence(userId);
        Document response = new Document("exists", exists);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Document> login(@RequestBody Document body) {
        String email = body.getString("email");
        String password = body.getString("password");
        return authService.authenticateUser(email, password);
    }

    @PostMapping("/create")
    public ResponseEntity<Document> createUser(@RequestBody Document body) {
        return authService.createUser(body);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Document> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return authService.sendResetCode(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Document> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");
        return authService.resetPassword(email, code, newPassword, confirmPassword);
    }

    @PatchMapping("/id")
    public ResponseEntity<Document> patchUserInfo(@RequestBody Document body, @RequestParam String userId) {
        return authService.patchUserInfo(body, userId);
    }

    @DeleteMapping("/id")
    public ResponseEntity<Document> deleteUser(@RequestParam String userId) {
        return authService.deleteUser(userId);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Document> validateToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        
        if (token == null || token.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "Token is required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        try {
            if (jwtService.validateToken(token)) {
                String email = jwtService.extractEmail(token);
                String userId = jwtService.extractUserId(token);
                String tokenType = jwtService.extractTokenType(token);
                
                Document response = new Document();
                response.put("valid", true);
                response.put("email", email);
                response.put("userId", userId);
                response.put("tokenType", tokenType);
                response.put("expiresAt", jwtService.getTokenExpirationTime(token));
                
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Invalid or expired token"), 
                    HttpStatus.UNAUTHORIZED
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Invalid token format"), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Document> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "Refresh token is required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        try {
            if (jwtService.validateToken(refreshToken) && jwtService.isRefreshToken(refreshToken)) {
                String email = jwtService.extractEmail(refreshToken);
                String userId = jwtService.extractUserId(refreshToken);
                
                // Generate new access token
                String newAccessToken = jwtService.generateToken(email, userId);
                
                Document response = new Document();
                response.put("accessToken", newAccessToken);
                response.put("tokenType", "Bearer");
                response.put("expiresIn", 86400); // 24 hours in seconds
                
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Invalid or expired refresh token"), 
                    HttpStatus.UNAUTHORIZED
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Invalid refresh token format"), 
                HttpStatus.BAD_REQUEST
            );
        }
    }
    
    /**
     * Social authentication endpoint (Facebook OAuth)
     * Frontend sends accessToken from Facebook, backend validates and creates/logs in user
     */
    @PostMapping("/social")
    public ResponseEntity<Document> socialAuth(@RequestBody Map<String, String> body) {
        String accessToken = body.get("accessToken");
        String provider = body.get("provider");
        
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "Access token is required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (provider == null || provider.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "Provider is required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        return authService.authenticateWithSocial(accessToken, provider);
    }
    
    /**
     * Complete user profile endpoint
     * Used after social auth to add required fields (preferredTeams, preferredLeagues)
     */
    @PutMapping("/complete-profile")
    public ResponseEntity<Document> completeProfile(@RequestBody Document body, @RequestParam String userId) {
        return authService.completeProfile(body, userId);
    }
}
