package com.leon.ideas.groups.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class UserValidationService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String AUTH_SERVICE_URL = "http://localhost:8080/football-pool/v1/api/auth";

    /**
     * Validate multiple emails and check if they exist in the system
     */
    public Map<String, Object> validateEmails(List<String> emails, String jwtToken) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> validatedUsers = new ArrayList<>();
        
        for (String email : emails) {
            Map<String, Object> userValidation = validateSingleEmail(email, jwtToken);
            validatedUsers.add(userValidation);
        }
        
        result.put("users", validatedUsers);
        result.put("total", emails.size());
        result.put("existing", validatedUsers.stream().filter(u -> (Boolean) u.get("exists")).count());
        result.put("nonExisting", validatedUsers.stream().filter(u -> !(Boolean) u.get("exists")).count());
        
        return result;
    }

    /**
     * Validate a single email
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> validateSingleEmail(String email, String jwtToken) {
        Map<String, Object> validation = new HashMap<>();
        validation.put("email", email);
        
        try {
            // Call auth service to check if user exists
            String url = AUTH_SERVICE_URL + "/check-email?email=" + email;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Boolean exists = (Boolean) body.get("exists");
                
                validation.put("exists", exists != null ? exists : false);
                
                if (exists != null && exists) {
                    validation.put("userId", body.get("userId"));
                    validation.put("name", body.get("name"));
                    validation.put("profileImage", body.get("profileImage"));
                }
            } else {
                validation.put("exists", false);
            }
        } catch (Exception e) {
            System.err.println("❌ Error validating email " + email + ": " + e.getMessage());
            validation.put("exists", false);
            validation.put("error", "Could not validate email");
        }
        
        return validation;
    }
}



