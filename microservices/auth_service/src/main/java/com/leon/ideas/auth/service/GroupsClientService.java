package com.leon.ideas.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GroupsClientService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${service.token}")
    private String serviceToken;

    private static final String GROUPS_SERVICE_URL = "http://localhost:1292/football-pool/v1/api/groups";
    private static final String COMPETITIONS_SERVICE_URL = "http://localhost:1291/football-pool/v1/api/competitions";

    /**
     * Get group information to extract competitionId and category
     * Made public to be used by AuthService
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getGroupInfo(String groupId) {
        try {
            String url = GROUPS_SERVICE_URL + "/internal/" + groupId;
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
                return (Map<String, Object>) response.getBody();
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting group info: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get match information with REAL results from competitions_service
     * This is the new approach: results are stored in competitions, not in groups
     */
    public Map<String, Object> getMatchFromCompetition(String groupId, String matchId) {
        try {
            // First, get group info to extract competitionId and category
            Map<String, Object> groupInfo = getGroupInfo(groupId);
            if (groupInfo == null) {
                System.err.println("❌ Group not found: " + groupId);
                return null;
            }

            String competitionId = (String) groupInfo.get("competitionId");
            String category = (String) groupInfo.get("category"); // We need to add this to Group model

            if (competitionId == null || category == null) {
                System.err.println("❌ Competition ID or category not found in group: " + groupId);
                // Fallback: try to get from competitions by searching
                return getMatchFromCompetitionFallback(competitionId, matchId);
            }

            // Get match from competitions_service
            String url = COMPETITIONS_SERVICE_URL + "/" + category + "/" + competitionId + "/matches/" + matchId;
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
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                if (body != null && body.containsKey("match")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> match = (Map<String, Object>) body.get("match");
                    return match;
                }
                return body;
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting match from competition: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Fallback: try to find match by searching all categories
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMatchFromCompetitionFallback(String competitionId, String matchId) {
        if (competitionId == null) {
            return null;
        }

        String[] categories = {"fifaNationalTeamCups", "fifaOfficialClubCups", "nationalClubLeagues"};
        for (String category : categories) {
            try {
                String url = COMPETITIONS_SERVICE_URL + "/" + category + "/" + competitionId + "/matches/" + matchId;
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
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    if (body != null && body.containsKey("match")) {
                        return (Map<String, Object>) body.get("match");
                    }
                }
            } catch (Exception e) {
                // Continue to next category
            }
        }
        return null;
    }

    /**
     * Get match information from a group (DEPRECATED - use getMatchFromCompetition instead)
     * Kept for backward compatibility
     */
    @Deprecated
    public Map<String, Object> getMatchFromGroup(String groupId, String matchId) {
        // Try new approach first
        Map<String, Object> match = getMatchFromCompetition(groupId, matchId);
        if (match != null) {
            return match;
        }

        // Fallback to old approach
        try {
            String url = GROUPS_SERVICE_URL + "/internal/matches/" + groupId + "/" + matchId;
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
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                if (body != null && body.containsKey("match")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> matchData = (Map<String, Object>) body.get("match");
                    return matchData;
                }
                return body;
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting match from group: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update scoreboard after calculating prediction points
     */
    public boolean updateScoreboardAfterPrediction(String groupId, String userId, Integer totalScore, String jwtToken) {
        try {
            String url = GROUPS_SERVICE_URL + "/" + groupId + "/update-scoreboard";
            HttpHeaders headers = new HttpHeaders();
            if (jwtToken != null) {
                headers.set("Authorization", "Bearer " + jwtToken);
            } else {
                headers.set("X-Service-Token", serviceToken);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "userId", userId,
                "totalScore", totalScore != null ? totalScore : 0
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("❌ Error updating scoreboard: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get match information from competitions_service by searching all categories
     * This is used when we don't have a groupId but need to find the match
     * Returns a map with the match data, competitionId, and category
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMatchFromCompetitionByMatchId(String matchId) {
        String[] categories = {"fifaNationalTeamCups", "fifaOfficialClubCups", "nationalClubLeagues"};
        
        try {
            // First, get all competitions from the main endpoint
            String allCompetitionsUrl = COMPETITIONS_SERVICE_URL;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> allCompetitionsResponse = restTemplate.exchange(
                allCompetitionsUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (allCompetitionsResponse.getStatusCode().is2xxSuccessful() && allCompetitionsResponse.getBody() != null) {
                Map<String, Object> allCompetitions = (Map<String, Object>) allCompetitionsResponse.getBody();
                
                // Search in each category
                for (String category : categories) {
                    if (!allCompetitions.containsKey(category)) {
                        continue;
                    }
                    
                    List<Map<String, Object>> competitionsList = (List<Map<String, Object>>) allCompetitions.get(category);
                    if (competitionsList == null) {
                        continue;
                    }
                    
                    // Search in each competition of this category
                    for (Map<String, Object> competition : competitionsList) {
                        String competitionId = (String) competition.get("id");
                        if (competitionId == null) {
                            continue;
                        }
                        
                        // Try to get the match from this competition
                        try {
                            String matchUrl = COMPETITIONS_SERVICE_URL + "/" + category + "/" + competitionId + "/matches/" + matchId;
                            ResponseEntity<Map> matchResponse = restTemplate.exchange(
                                matchUrl,
                                HttpMethod.GET,
                                entity,
                                Map.class
                            );
                            
                            if (matchResponse.getStatusCode().is2xxSuccessful() && matchResponse.getBody() != null) {
                                Map<String, Object> matchBody = (Map<String, Object>) matchResponse.getBody();
                                if (matchBody != null && matchBody.containsKey("match")) {
                                    Map<String, Object> match = (Map<String, Object>) matchBody.get("match");
                                    // Add competitionId and category to the match data
                                    match.put("competitionId", competitionId);
                                    match.put("category", category);
                                    System.out.println("✅ Match found: " + matchId + " in competition: " + competitionId + " (" + category + ")");
                                    return match;
                                }
                            }
                        } catch (Exception e) {
                            // Match not found in this competition, continue to next
                            continue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error searching for match " + matchId + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        System.err.println("❌ Match not found: " + matchId);
        return null;
    }
    
    /**
     * Get match directly from competitions_service using competitionId and category
     * This is used when we already know the competitionId and category (e.g., from frontend)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMatchFromCompetitionDirect(String competitionId, String category, String matchId) {
        try {
            String url = COMPETITIONS_SERVICE_URL + "/" + category + "/" + competitionId + "/matches/" + matchId;
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
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                if (body != null && body.containsKey("match")) {
                    Map<String, Object> match = (Map<String, Object>) body.get("match");
                    // Add competitionId and category to the match data
                    match.put("competitionId", competitionId);
                    match.put("category", category);
                    return match;
                }
                return body;
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting match from competition (direct): " + e.getMessage());
            // Don't print stack trace for 404 errors (match not found)
            if (!e.getMessage().contains("404")) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /**
     * Get group info including name, competitionId, and category
     * Returns a map with: groupId, name, competitionId, category
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getGroupInfoInternal(String groupId) {
        try {
            String url = GROUPS_SERVICE_URL + "/internal/" + groupId;
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
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                // The endpoint returns the groupInfo directly
                return body;
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting group info (internal): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Update group with user prediction (add or update UserPrediction in group.userPredictions[])
     */
    public boolean updateGroupPrediction(String groupId, String userId, String matchId, Integer team1Score, Integer team2Score,
                                        Boolean userExtraTime, Boolean userPenalties, Integer userPenaltiesTeam1Score, Integer userPenaltiesTeam2Score,
                                        Integer points) {
        try {
            String url = GROUPS_SERVICE_URL + "/internal/" + groupId + "/predictions";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("userId", userId);
            body.put("matchId", matchId);
            body.put("userTeam1Score", team1Score);
            body.put("userTeam2Score", team2Score);
            if (userExtraTime != null) {
                body.put("userExtraTime", userExtraTime);
            }
            if (userPenalties != null) {
                body.put("userPenalties", userPenalties);
            }
            if (userPenaltiesTeam1Score != null) {
                body.put("userPenaltiesTeam1Score", userPenaltiesTeam1Score);
            }
            if (userPenaltiesTeam2Score != null) {
                body.put("userPenaltiesTeam2Score", userPenaltiesTeam2Score);
            }
            if (points != null) {
                body.put("points", points);
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("❌ Error updating group prediction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Update group with matchesDetail and user score
     * This updates the group with:
     * - matchesDetail: array of match info (same as matchInfo from predictions)
     * - users[].score: user's accumulated score for the competition
     */
    @SuppressWarnings("unchecked")
    public boolean updateGroupWithMatchesDetail(String groupId, String userId, String competitionId, 
                                                List<Map<String, Object>> matchesDetail, Integer userScore) {
        try {
            String url = GROUPS_SERVICE_URL + "/internal/" + groupId + "/update-matches-detail";
            System.out.println("📤 Calling groups_service to update matchesDetail: " + url);
            System.out.println("   groupId: " + groupId);
            System.out.println("   userId: " + userId);
            System.out.println("   competitionId: " + competitionId);
            System.out.println("   matchesDetail size: " + (matchesDetail != null ? matchesDetail.size() : 0));
            System.out.println("   userScore: " + userScore);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("userId", userId);
            body.put("competitionId", competitionId);
            body.put("matchesDetail", matchesDetail);
            body.put("userScore", userScore);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                System.out.println("✅ Successfully updated group " + groupId + " with matchesDetail");
            } else {
                System.err.println("❌ Failed to update group " + groupId + ". Status: " + response.getStatusCode());
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("❌ Error updating group with matchesDetail: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Update multiple groups with matchesDetail and user score
     * This updates each group with:
     * - users[].score: user's accumulated score for the competition
     * - users[].matchesInfo: array of match info (same as matchInfo from predictions)
     */
    @SuppressWarnings("unchecked")
    public boolean updateMultipleGroupsWithMatchesDetail(List<String> groupIds, String userId, String competitionId, 
                                                         List<Map<String, Object>> matchesDetail, Integer userScore) {
        String url = GROUPS_SERVICE_URL + "/internal/update-matches-detail-multiple";
        try {
            System.out.println("📤 Calling groups_service to update multiple groups with matchesDetail: " + url);
            System.out.println("   groupIds count: " + (groupIds != null ? groupIds.size() : 0));
            System.out.println("   userId: " + userId);
            System.out.println("   competitionId: " + competitionId);
            System.out.println("   matchesDetail size: " + (matchesDetail != null ? matchesDetail.size() : 0));
            System.out.println("   userScore: " + userScore);
            
            HttpHeaders headers = new HttpHeaders();
            System.out.println("   🔐 Setting X-Service-Token header");
            System.out.println("   serviceToken: " + (serviceToken != null ? "PRESENT (length: " + serviceToken.length() + ", starts with: " + (serviceToken.length() > 5 ? serviceToken.substring(0, 5) + "..." : serviceToken) + ")" : "NULL"));
            if (serviceToken == null || serviceToken.trim().isEmpty()) {
                System.err.println("   ❌ ERROR: serviceToken is NULL or EMPTY!");
            }
            headers.set("X-Service-Token", serviceToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            System.out.println("   ✅ Headers configured");
            
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("groupIds", groupIds);
            body.put("userId", userId);
            body.put("competitionId", competitionId);
            body.put("matchesDetail", matchesDetail);
            body.put("userScore", userScore);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            System.out.println("   Making HTTP POST request to: " + url);
            System.out.println("   Request body keys: " + body.keySet());
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response;
            try {
                response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
                );
                System.out.println("   Response status: " + response.getStatusCode());
            } catch (Exception e) {
                System.err.println("❌ Exception calling groups_service: " + e.getMessage());
                System.err.println("   URL: " + url);
                System.err.println("   Error type: " + e.getClass().getName());
                e.printStackTrace();
                throw e; // Re-throw to be caught by AuthService
            }
            
            System.out.println("   Response received - Status: " + response.getStatusCode());
            System.out.println("   Response body: " + response.getBody());
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    System.out.println("   Response body keys: " + responseBody.keySet());
                    Object successCountObj = responseBody.get("successCount");
                    Object errorCountObj = responseBody.get("errorCount");
                    int successCount = successCountObj != null ? ((Number) successCountObj).intValue() : 0;
                    int errorCount = errorCountObj != null ? ((Number) errorCountObj).intValue() : 0;
                    System.out.println("   ✅ Successfully updated " + successCount + " groups with matchesDetail");
                    if (errorCount > 0) {
                        System.err.println("   ⚠️ " + errorCount + " groups failed to update");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
                        if (results != null) {
                            for (Map<String, Object> result : results) {
                                if ("error".equals(result.get("status"))) {
                                    System.err.println("      ❌ Group " + result.get("groupId") + ": " + result.get("message"));
                                }
                            }
                        }
                    }
                    // Return true if at least one group was updated successfully
                    boolean result = successCount > 0;
                    System.out.println("   Returning: " + result);
                    return result;
                } else {
                    System.err.println("   ❌ Response body is null");
                    return false;
                }
            } else {
                System.err.println("   ❌ Failed to update groups. Status: " + response.getStatusCode());
                if (response.getBody() != null) {
                    System.err.println("   Response body: " + response.getBody());
                } else {
                    System.err.println("   Response body is null");
                }
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating multiple groups with matchesDetail: " + e.getMessage());
            e.printStackTrace();
            System.err.println("   URL: " + url);
            System.err.println("   groupIds: " + groupIds);
            System.err.println("   userId: " + userId);
            System.err.println("   competitionId: " + competitionId);
            return false;
        }
    }
}
