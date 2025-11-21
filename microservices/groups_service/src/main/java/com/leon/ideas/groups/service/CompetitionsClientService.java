package com.leon.ideas.groups.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CompetitionsClientService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String COMPETITIONS_SERVICE_URL = "http://localhost:8080/football-pool/v1/api/competitions";

    /**
     * Get competition details and qualified teams
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCompetitionWithTeams(String category, String competitionId, String jwtToken) {
        try {
            // Get competition details
            String url = COMPETITIONS_SERVICE_URL + "/" + category + "/" + competitionId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error fetching competition: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get qualified teams for a competition
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getQualifiedTeams(String category, String competitionId, String jwtToken) {
        try {
            String url = COMPETITIONS_SERVICE_URL + "/" + category + "/" + competitionId + "/teams";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error fetching qualified teams: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}


