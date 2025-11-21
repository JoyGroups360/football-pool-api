package com.leon.ideas.groups.controller;

import com.leon.ideas.groups.service.GroupsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/football-pool/v1/api/groups")
public class GroupsController {

    @Autowired
    private GroupsService groupsService;

    /**
     * POST /football-pool/v1/api/groups
     * Create a new group
     * 
     * Required fields in body:
     * - competitionId (String)
     * - category (String) - fifaNationalTeamCups, fifaOfficialClubCups, or nationalClubLeagues
     * - name (String) - group name
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createGroup(
            @RequestBody Map<String, Object> groupData,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        String userEmail = (String) request.getAttribute("userEmail");
        String jwtToken = request.getHeader("Authorization").substring(7);
        
        System.out.println("➕ POST request received: Create group");
        System.out.println("User ID: " + userId);
        System.out.println("Group data: " + groupData);
        
        return groupsService.createGroup(userId, userEmail, groupData, jwtToken);
    }

    /**
     * GET /football-pool/v1/api/groups/:id
     * Get group by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getGroupById(
            @PathVariable String id,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("📋 GET request received: Get group by ID - " + id);
        System.out.println("User ID: " + userId);
        
        return groupsService.getGroupById(id, userId);
    }

    /**
     * GET /football-pool/v1/api/groups
     * Get all groups for the authenticated user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserGroups(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("📋 GET request received: Get user groups");
        System.out.println("User ID: " + userId);
        
        return groupsService.getUserGroups(userId);
    }

    /**
     * PUT /football-pool/v1/api/groups/:id
     * Update a group (full update)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("✏️ PUT request received: Update group - " + id);
        System.out.println("User ID: " + userId);
        System.out.println("Updates: " + updates);
        
        return groupsService.updateGroup(id, userId, updates);
    }

    /**
     * PATCH /football-pool/v1/api/groups/:id
     * Patch a group (partial update)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> patchGroup(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("🔧 PATCH request received: Patch group - " + id);
        System.out.println("User ID: " + userId);
        System.out.println("Updates: " + updates);
        
        return groupsService.patchGroup(id, userId, updates);
    }

    /**
     * DELETE /football-pool/v1/api/groups/:id
     * Delete a group
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteGroup(
            @PathVariable String id,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("🗑️ DELETE request received: Delete group - " + id);
        System.out.println("User ID: " + userId);
        
        return groupsService.deleteGroup(id, userId);
    }

    /**
     * POST /football-pool/v1/api/groups/:id/invite
     * Send invitation to join group
     * 
     * Required fields in body:
     * - email (String) - email to invite
     */
    @PostMapping("/{id}/invite")
    public ResponseEntity<Map<String, Object>> inviteToGroup(
            @PathVariable String id,
            @RequestBody Map<String, Object> inviteData,
            HttpServletRequest request) {
        
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("📧 ENDPOINT CALLED: POST /groups/{id}/invite");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("📧 Group ID from path: " + id);
        System.out.println("📧 Request body: " + inviteData);
        
        String userId = (String) request.getAttribute("userId");
        String userEmail = (String) request.getAttribute("userEmail");
        
        System.out.println("📧 User ID: " + userId);
        System.out.println("📧 User Email: " + userEmail);
        
        if (userId == null || userEmail == null) {
            System.err.println("❌ ERROR: userId or userEmail is NULL!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "User not authenticated"
            ));
        }
        
        return groupsService.inviteToGroup(id, userId, userEmail, inviteData);
    }

    /**
     * POST /football-pool/v1/api/groups/:id/join
     * Join a group (for invited users)
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<Map<String, Object>> joinGroup(
            @PathVariable String id,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        String userEmail = (String) request.getAttribute("userEmail");
        String jwtToken = request.getHeader("Authorization").substring(7);
        
        System.out.println("🤝 POST request received: Join group - " + id);
        System.out.println("User ID: " + userId);
        
        return groupsService.joinGroup(id, userId, userEmail, jwtToken);
    }

    /**
     * POST /football-pool/v1/api/groups/validate-emails
     * Validate if emails exist in the system
     * 
     * Required fields in body:
     * - emails (Array<String>) - list of emails to validate
     */
    @PostMapping("/validate-emails")
    public ResponseEntity<Map<String, Object>> validateEmails(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        
        String jwtToken = request.getHeader("Authorization").substring(7);
        @SuppressWarnings("unchecked")
        List<String> emails = (List<String>) requestData.get("emails");
        
        System.out.println("✅ POST request received: Validate emails");
        System.out.println("Emails to validate: " + emails);
        
        return groupsService.validateEmails(emails, jwtToken);
    }

    /**
     * POST /football-pool/v1/api/groups/test-email
     * Test email endpoint (for debugging)
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, Object>> testEmail(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        
        String toEmail = (String) requestData.get("email");
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Email is required"
            ));
        }
        
        System.out.println("🧪 TEST EMAIL endpoint called for: " + toEmail);
        
        return groupsService.testEmail(toEmail);
    }

    /**
     * GET /football-pool/v1/api/groups/by-invited-email?email={email}
     * Used internally by auth_service when a user registers to find groups that invited this email.
     */
    @GetMapping("/by-invited-email")
    public ResponseEntity<Map<String, Object>> getGroupsByInvitedEmail(
            @RequestParam String email
    ) {
        System.out.println("🔍 GET groups by invited email: " + email);
        return groupsService.getGroupsByInvitedEmail(email);
    }

    /**
     * GET /football-pool/v1/api/groups/{groupId}/matches
     * Get all matches for a group
     * 
     * Optional query parameters:
     * - stageId: Filter by stage (e.g., "group-stage", "round-of-16")
     * - groupLetter: Filter by group letter (e.g., "A", "B") - only for group stage
     * - status: Filter by match status (e.g., "scheduled", "finished")
     */
    @GetMapping("/{groupId}/matches")
    public ResponseEntity<Map<String, Object>> getMatches(
            @PathVariable String groupId,
            @RequestParam(required = false) String stageId,
            @RequestParam(required = false) String groupLetter,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("⚽ GET /groups/{groupId}/matches - ENDPOINT CALLED");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Group ID: " + groupId);
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Request Method: " + request.getMethod());
        System.out.println("Authorization Header: " + (request.getHeader("Authorization") != null ? "PRESENT" : "MISSING"));
        
        String userId = (String) request.getAttribute("userId");
        String userEmail = (String) request.getAttribute("userEmail");
        
        System.out.println("User ID from request attribute: " + userId);
        System.out.println("User Email from request attribute: " + userEmail);
        System.out.println("Stage ID filter: " + stageId);
        System.out.println("Group letter filter: " + groupLetter);
        System.out.println("Status filter: " + status);
        
        if (userId == null) {
            System.err.println("❌ ERROR: userId is NULL! Authentication may have failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Unauthorized",
                "message", "User ID not found in request. Please ensure you are authenticated."
            ));
        }
        
        System.out.println("✅ Proceeding to get matches...");
        return groupsService.getMatches(groupId, userId, stageId, groupLetter, status);
    }

    /**
     * GET /football-pool/v1/api/groups/{groupId}/matches/{matchId}
     * Get a specific match by ID
     */
    @GetMapping("/{groupId}/matches/{matchId}")
    public ResponseEntity<Map<String, Object>> getMatchById(
            @PathVariable String groupId,
            @PathVariable String matchId,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("⚽ GET request received: Get match - " + matchId + " from group - " + groupId);
        
        return groupsService.getMatchById(groupId, matchId, userId);
    }

    /**
     * POST /football-pool/v1/api/groups/{groupId}/matches/{matchId}/result
     * Register or update match result
     * 
     * Required fields in body:
     * - team1Score (Integer)
     * - team2Score (Integer)
     * 
     * Optional fields:
     * - playedDate (Date/String) - When the match was played
     * - venue (String) - Stadium/venue name
     */
    @PostMapping("/{groupId}/matches/{matchId}/result")
    public ResponseEntity<Map<String, Object>> registerMatchResult(
            @PathVariable String groupId,
            @PathVariable String matchId,
            @RequestBody Map<String, Object> resultData,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("⚽ POST request received: Register match result");
        System.out.println("Group ID: " + groupId);
        System.out.println("Match ID: " + matchId);
        System.out.println("Result data: " + resultData);
        System.out.println("User ID: " + userId);
        
        return groupsService.registerMatchResult(groupId, matchId, userId, resultData);
    }

    /**
     * PUT /football-pool/v1/api/groups/{groupId}/matches/{matchId}/result
     * Update match result (same as POST, but semantically for updates)
     */
    @PutMapping("/{groupId}/matches/{matchId}/result")
    public ResponseEntity<Map<String, Object>> updateMatchResult(
            @PathVariable String groupId,
            @PathVariable String matchId,
            @RequestBody Map<String, Object> resultData,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("⚽ PUT request received: Update match result");
        System.out.println("Group ID: " + groupId);
        System.out.println("Match ID: " + matchId);
        System.out.println("Result data: " + resultData);
        
        return groupsService.registerMatchResult(groupId, matchId, userId, resultData);
    }

    /**
     * DELETE /football-pool/v1/api/groups/{groupId}/matches/{matchId}/result
     * Delete/clear match result (reset match to not played)
     */
    @DeleteMapping("/{groupId}/matches/{matchId}/result")
    public ResponseEntity<Map<String, Object>> clearMatchResult(
            @PathVariable String groupId,
            @PathVariable String matchId,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        
        System.out.println("⚽ DELETE request received: Clear match result");
        System.out.println("Group ID: " + groupId);
        System.out.println("Match ID: " + matchId);
        
        return groupsService.clearMatchResult(groupId, matchId, userId);
    }
    
    /**
     * POST /football-pool/v1/api/groups/{groupId}/matches/{matchId}/predict
     * Save or update a match prediction for the authenticated user
     *
     * Required fields in body:
     * - team1Score (Integer)
     * - team2Score (Integer)
     */
    @PostMapping("/{groupId}/matches/{matchId}/predict")
    public ResponseEntity<Map<String, Object>> savePrediction(
            @PathVariable String groupId,
            @PathVariable String matchId,
            @RequestBody Map<String, Object> predictionData,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        String jwtToken = request.getHeader("Authorization").substring(7);
        
        System.out.println("🎯 POST request received: Save prediction");
        System.out.println("Group ID: " + groupId);
        System.out.println("Match ID: " + matchId);
        System.out.println("User ID: " + userId);
        System.out.println("Prediction data: " + predictionData);
        
        return groupsService.savePrediction(groupId, matchId, userId, predictionData, jwtToken);
    }
    
    /**
     * GET /football-pool/v1/api/groups/{groupId}/predictions
     * Get all predictions for the authenticated user in a group
     */
    @GetMapping("/{groupId}/predictions")
    public ResponseEntity<Map<String, Object>> getUserPredictions(
            @PathVariable String groupId,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        String jwtToken = request.getHeader("Authorization").substring(7);
        
        System.out.println("🎯 GET request received: Get user predictions");
        System.out.println("Group ID: " + groupId);
        System.out.println("User ID: " + userId);
        
        return groupsService.getUserPredictions(groupId, userId, jwtToken);
    }
    
    /**
     * GET /football-pool/v1/api/groups/{groupId}/matches/{matchId}/predict
     * Get a specific prediction for the authenticated user, group, and match
     */
    @GetMapping("/{groupId}/matches/{matchId}/predict")
    public ResponseEntity<Map<String, Object>> getUserPrediction(
            @PathVariable String groupId,
            @PathVariable String matchId,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        String jwtToken = request.getHeader("Authorization").substring(7);
        
        System.out.println("🎯 GET request received: Get user prediction");
        System.out.println("Group ID: " + groupId);
        System.out.println("Match ID: " + matchId);
        System.out.println("User ID: " + userId);
        
        return groupsService.getUserPrediction(groupId, matchId, userId, jwtToken);
    }
    
    /**
     * POST /football-pool/v1/api/groups/{groupId}/calculate-scores
     * Calculate scores for all users' predictions in a group
     * Compares predictions vs actual results and assigns points
     * Only the group creator can trigger this
     */
    @PostMapping("/{groupId}/calculate-scores")
    public ResponseEntity<Map<String, Object>> calculateScores(
            @PathVariable String groupId,
            HttpServletRequest request) {
        
        String userId = (String) request.getAttribute("userId");
        String jwtToken = request.getHeader("Authorization").substring(7);
        
        System.out.println("📊 POST request received: Calculate scores");
        System.out.println("Group ID: " + groupId);
        System.out.println("User ID: " + userId);
        
        return groupsService.calculateScores(groupId, userId, jwtToken);
    }
    
    /**
     * POST /football-pool/v1/api/groups/{groupId}/payment/{userId}
     * Update user payment status after successful payment
     * Called by payments service after payment confirmation
     * 
     * Required fields in body:
     * - paymentId (String) - payment ID from payments service
     */
    @PostMapping("/{groupId}/payment/{userId}")
    public ResponseEntity<Map<String, Object>> updateUserPayment(
            @PathVariable String groupId,
            @PathVariable String userId,
            @RequestBody Map<String, Object> paymentData,
            HttpServletRequest request) {
        
        String paymentId = (String) paymentData.get("paymentId");
        if (paymentId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "paymentId is required"
            ));
        }
        
        System.out.println("💳 POST request received: Update user payment");
        System.out.println("Group ID: " + groupId);
        System.out.println("User ID: " + userId);
        System.out.println("Payment ID: " + paymentId);
        
        return groupsService.updateUserPayment(groupId, userId, paymentId);
    }
}

