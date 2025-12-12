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
    public ResponseEntity<Document> getUsers(
            @RequestHeader(value = "X-Service-Token", required = false) String serviceTokenHeader) {
        // If service token is provided, validate it (for internal calls)
        if (serviceTokenHeader != null && !serviceTokenHeader.trim().isEmpty()) {
            // Validate service token
            if (!serviceTokenHeader.equals(authService.getServiceToken())) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid service token"),
                    HttpStatus.UNAUTHORIZED
                );
            }
        }
        
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

    /**
     * POST /football-pool/v1/api/auth/{userId}/groups/{groupId}
     * Add a groupId to the user's groups array
     */
    @PostMapping("/{userId}/groups/{groupId}")
    public ResponseEntity<Document> addGroupToUser(
            @PathVariable String userId,
            @PathVariable String groupId
    ) {
        return authService.addGroupToUser(userId, groupId);
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

    @GetMapping("/check-email")
    public ResponseEntity<Document> checkEmail(@RequestParam String email) {
        System.out.println("✅ Checking if email exists: " + email);
        return authService.checkEmailExists(email);
    }
    
    /**
     * POST /football-pool/v1/api/auth/predictions
     * Save or update a match prediction for a user
     * userId must be sent in the body, not in the URL
     */
    @PostMapping("/predictions")
    public ResponseEntity<Document> savePrediction(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🎯 POST /predictions - REQUEST RECEIVED");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Request body keys: " + body.keySet());
        
        // Try to get userId from JWT token first (more secure)
        String userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                userId = jwtService.extractUserId(token);
                System.out.println("✅ UserId extracted from JWT token: " + userId);
            } catch (Exception e) {
                System.err.println("⚠️ Could not extract userId from token: " + e.getMessage());
            }
        }
        
        // Fallback: Get userId from body (for backward compatibility)
        if (userId == null || userId.trim().isEmpty()) {
            userId = (String) body.get("userId");
            if (userId != null && !userId.trim().isEmpty()) {
                System.out.println("✅ UserId obtained from request body: " + userId);
            }
        }
        
        // Validate userId is present
        if (userId == null || userId.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "userId is required. Please include Authorization header with valid JWT token, or send userId in the body"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        String matchId = (String) body.get("matchId");
        
        // Get groupIds array from frontend (REQUIRED)
        @SuppressWarnings("unchecked")
        List<String> groupIds = (List<String>) body.get("groupIds");
        if (groupIds == null || groupIds.isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "groupIds array is required"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        String competitionId = (String) body.get("competitionId"); // REQUIRED now
        Integer team1Score = body.get("team1Score") != null ? 
            (body.get("team1Score") instanceof Integer ? (Integer) body.get("team1Score") : 
             ((Number) body.get("team1Score")).intValue()) : null;
        Integer team2Score = body.get("team2Score") != null ? 
            (body.get("team2Score") instanceof Integer ? (Integer) body.get("team2Score") : 
             ((Number) body.get("team2Score")).intValue()) : null;
        Integer realTeam1Score = body.get("realTeam1Score") != null ? 
            (body.get("realTeam1Score") instanceof Integer ? (Integer) body.get("realTeam1Score") : 
             ((Number) body.get("realTeam1Score")).intValue()) : null;
        Integer realTeam2Score = body.get("realTeam2Score") != null ? 
            (body.get("realTeam2Score") instanceof Integer ? (Integer) body.get("realTeam2Score") : 
             ((Number) body.get("realTeam2Score")).intValue()) : null;
        Boolean extraTime = body.get("extraTime") != null ? 
            (body.get("extraTime") instanceof Boolean ? (Boolean) body.get("extraTime") : 
             Boolean.parseBoolean(body.get("extraTime").toString())) : null;
        Boolean realExtraTime = body.get("realExtraTime") != null ? 
            (body.get("realExtraTime") instanceof Boolean ? (Boolean) body.get("realExtraTime") : 
             Boolean.parseBoolean(body.get("realExtraTime").toString())) : null;
        Integer penaltiesteam1Score = body.get("penaltiesteam1Score") != null ? 
            (body.get("penaltiesteam1Score") instanceof Integer ? (Integer) body.get("penaltiesteam1Score") : 
             ((Number) body.get("penaltiesteam1Score")).intValue()) : null;
        Integer penaltiesteam2Score = body.get("penaltiesteam2Score") != null ? 
            (body.get("penaltiesteam2Score") instanceof Integer ? (Integer) body.get("penaltiesteam2Score") : 
             ((Number) body.get("penaltiesteam2Score")).intValue()) : null;
        
        // Get team1 and team2 from frontend (REQUIRED)
        String team1 = (String) body.get("team1");
        String team2 = (String) body.get("team2");
        
        return authService.savePrediction(userId, groupIds, matchId, team1Score, team2Score,
                                         realTeam1Score, realTeam2Score,
                                         extraTime, realExtraTime,
                                         penaltiesteam1Score, penaltiesteam2Score,
                                         competitionId, team1, team2);
    }
    
    /**
     * GET /football-pool/v1/api/auth/{userId}/predictions
     * Get all predictions for a user, optionally filtered by groupId
     * Supports both JWT token (Authorization header) and service token (X-Service-Token header) for internal calls
     */
    @GetMapping("/{userId}/predictions")
    public ResponseEntity<Document> getUserPredictions(
            @PathVariable String userId,
            @RequestParam(required = false) String groupId,
            @RequestHeader(value = "X-Service-Token", required = false) String serviceTokenHeader) {
        // If service token is provided, validate it (for internal calls)
        if (serviceTokenHeader != null && !serviceTokenHeader.trim().isEmpty()) {
            // Validate service token
            if (!serviceTokenHeader.equals(authService.getServiceToken())) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid service token"),
                    HttpStatus.UNAUTHORIZED
                );
            }
        }
        return authService.getUserPredictions(userId, groupId);
    }
    
    /**
     * GET /football-pool/v1/api/auth/{userId}/predictions/{groupId}/{matchId}
     * Get a specific prediction for a user, group, and match
     */
    @GetMapping("/{userId}/predictions/{groupId}/{matchId}")
    public ResponseEntity<Document> getUserPrediction(
            @PathVariable String userId,
            @PathVariable String groupId,
            @PathVariable String matchId) {
        return authService.getUserPrediction(userId, groupId, matchId);
    }
    
    /**
     * PUT /football-pool/v1/api/auth/{userId}/predictions/{competitionId}/{matchId}/points
     * Update prediction points after calculating scores
     */
    @PutMapping("/{userId}/predictions/{competitionId}/{matchId}/points")
    public ResponseEntity<Document> updatePredictionPoints(
            @PathVariable String userId,
            @PathVariable String competitionId,
            @PathVariable String matchId) {
        return authService.updatePredictionPoints(userId, competitionId, matchId);
    }
    
    /**
     * GET /football-pool/v1/api/auth/internal/predictions/{userId}/{groupId}/{matchId}
     * Internal endpoint for groups_service to get predictions using service token
     * Only accessible with valid service token in X-Service-Token header
     */
    @GetMapping("/internal/predictions/{userId}/{groupId}/{matchId}")
    public ResponseEntity<Document> getPredictionInternal(
            @PathVariable String userId,
            @PathVariable String groupId,
            @PathVariable String matchId,
            @RequestHeader(value = "X-Service-Token", required = false) String serviceToken) {
        return authService.getUserPredictionInternal(userId, groupId, matchId, serviceToken);
    }
}
