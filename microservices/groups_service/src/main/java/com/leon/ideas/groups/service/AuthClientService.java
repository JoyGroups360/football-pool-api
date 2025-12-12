package com.leon.ideas.groups.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AuthClientService {

    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${service.token}")
    private String serviceToken;

    private static final String AUTH_SERVICE_URL = "http://localhost:8080/football-pool/v1/api/auth";

    /**
     * Add a groupId to a user's groups array in auth_service.
     */
    public void addGroupToUser(String userId, String groupId, String jwtToken) {
        if (userId == null || groupId == null || jwtToken == null) {
            return;
        }

        try {
            String url = AUTH_SERVICE_URL + "/" + userId + "/groups/" + groupId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(null, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("✅ addGroupToUser response (" + userId + " -> " + groupId + "): " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("❌ Error calling auth_service to add group to user " + userId + ": " + e.getMessage());
        }
    }
    
    /**
     * Save or update a match prediction for a user
     */
    public Map<String, Object> savePrediction(String userId, String groupId, String matchId, Integer team1Score, Integer team2Score, String jwtToken) {
        try {
            String url = AUTH_SERVICE_URL + "/" + userId + "/predictions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = Map.of(
                "groupId", groupId,
                "matchId", matchId,
                "team1Score", team1Score,
                "team2Score", team2Score
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            System.err.println("❌ Error saving prediction: " + e.getMessage());
            e.printStackTrace();
        }
        return Map.of("error", "Failed to save prediction");
    }
    
    /**
     * Get all predictions for a user, optionally filtered by groupId
     * If jwtToken is null, uses service token for internal calls
     */
    public Map<String, Object> getUserPredictions(String userId, String groupId, String jwtToken) {
        try {
            String url = AUTH_SERVICE_URL + "/" + userId + "/predictions";
            if (groupId != null && !groupId.trim().isEmpty()) {
                url += "?groupId=" + groupId;
            }
            
            HttpHeaders headers = new HttpHeaders();
            if (jwtToken != null) {
                headers.set("Authorization", "Bearer " + jwtToken);
            } else {
                // Use service token for internal calls
                headers.set("X-Service-Token", serviceToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting predictions: " + e.getMessage());
            e.printStackTrace();
        }
        return Map.of("predictions", List.of(), "count", 0);
    }
    
    /**
     * Get a specific prediction for a user, group, and match
     * Uses JWT token if provided, otherwise uses service token for internal calls
     */
    public Map<String, Object> getUserPrediction(String userId, String groupId, String matchId, String jwtToken) {
        // If jwtToken is null, use service token for internal calls
        if (jwtToken == null) {
            return getUserPredictionInternal(userId, groupId, matchId);
        }
        
        try {
            String url = AUTH_SERVICE_URL + "/" + userId + "/predictions/" + groupId + "/" + matchId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting prediction: " + e.getMessage());
            e.printStackTrace();
        }
        return Map.of("exists", false, "prediction", null);
    }
    
    /**
     * Get a specific prediction using service token (for internal/scheduled tasks)
     */
    private Map<String, Object> getUserPredictionInternal(String userId, String groupId, String matchId) {
        try {
            String url = AUTH_SERVICE_URL + "/internal/predictions/" + userId + "/" + groupId + "/" + matchId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting prediction with service token: " + e.getMessage());
            e.printStackTrace();
        }
        return Map.of("exists", false, "prediction", null);
    }
    
    /**
     * Update prediction points after calculating scores
     */
    public void updatePredictionPoints(String userId, String groupId, String matchId, Integer points, String jwtToken) {
        try {
            String url = AUTH_SERVICE_URL + "/" + userId + "/predictions/" + groupId + "/" + matchId + "/points";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = Map.of("points", points);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                String.class
            );
        } catch (Exception e) {
            System.err.println("❌ Error updating prediction points: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get user information by userId (name, email, groupScores, etc.)
     * If jwtToken is null, uses service token for internal calls
     */
    public Map<String, Object> getUserById(String userId, String jwtToken) {
        try {
            // Try to get user from getAllUsers and filter by userId
            String url = AUTH_SERVICE_URL;
            
            HttpHeaders headers = new HttpHeaders();
            if (jwtToken != null) {
                headers.set("Authorization", "Bearer " + jwtToken);
            } else {
                // Use service token for internal calls
                headers.set("X-Service-Token", serviceToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
                
                if (users != null) {
                    for (Map<String, Object> user : users) {
                        String user_id = user.get("_id") != null ? user.get("_id").toString() : null;
                        if (userId.equals(user_id)) {
                            return user;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting user by ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Map.of("error", "User not found");
    }
}


