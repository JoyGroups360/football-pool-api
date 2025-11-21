package com.leon.ideas.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Service
public class FacebookAuthService {

    private static final Logger logger = LoggerFactory.getLogger(FacebookAuthService.class);
    private static final String FACEBOOK_GRAPH_API_URL = "https://graph.facebook.com/me";
    
    private final RestTemplate restTemplate;

    public FacebookAuthService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Validates Facebook access token and retrieves user information
     * 
     * @param accessToken The Facebook access token from the frontend
     * @return Map containing user data from Facebook (id, name, email, picture)
     * @throws RuntimeException if token is invalid or Facebook API fails
     */
    public Map<String, Object> validateTokenAndGetUserInfo(String accessToken) {
        try {
            String url = String.format(
                "%s?access_token=%s&fields=id,name,email,picture.type(large)",
                FACEBOOK_GRAPH_API_URL,
                accessToken
            );
            
            logger.info("🔍 Validating Facebook token...");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !response.containsKey("id")) {
                logger.error("❌ Invalid response from Facebook API");
                throw new RuntimeException("Invalid response from Facebook API");
            }
            
            logger.info("✅ Facebook token validated successfully for user: {}", response.get("email"));
            
            return response;
            
        } catch (HttpClientErrorException e) {
            logger.error("❌ Facebook token validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid Facebook access token", e);
        } catch (Exception e) {
            logger.error("❌ Error communicating with Facebook API: {}", e.getMessage());
            throw new RuntimeException("Error validating Facebook token", e);
        }
    }
    
    /**
     * Extracts the profile picture URL from Facebook response
     * 
     * @param facebookResponse The response from Facebook Graph API
     * @return The profile picture URL or null if not available
     */
    @SuppressWarnings("unchecked")
    public String extractProfilePicture(Map<String, Object> facebookResponse) {
        try {
            if (facebookResponse.containsKey("picture")) {
                Map<String, Object> picture = (Map<String, Object>) facebookResponse.get("picture");
                if (picture.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) picture.get("data");
                    return (String) data.get("url");
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ Could not extract profile picture from Facebook response");
        }
        return null;
    }
    
    /**
     * Splits Facebook's full name into first name and last name
     * 
     * @param fullName The full name from Facebook (e.g., "Joel Leon Gonzalez")
     * @return Array with [firstName, lastName]
     */
    public String[] splitName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[]{"", ""};
        }
        
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";
        
        return new String[]{firstName, lastName};
    }
}




