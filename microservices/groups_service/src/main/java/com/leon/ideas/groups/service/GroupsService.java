package com.leon.ideas.groups.service;

import com.leon.ideas.groups.model.Group;
import com.leon.ideas.groups.repository.GroupsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Calendar;

@Service
public class GroupsService {

    @Autowired
    private GroupsRepository groupsRepository;

    @Autowired
    private CompetitionsClientService competitionsClient;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserValidationService userValidationService;

    @Autowired
    private AuthClientService authClientService;

    /**
     * Create a new group
     */
    public ResponseEntity<Map<String, Object>> createGroup(String userId, String userEmail, Map<String, Object> groupData, String jwtToken) {
        try {
            String competitionId = (String) groupData.get("competitionId");
            String category = (String) groupData.get("category");
            String groupName = (String) groupData.get("name");
            
            // Get optional invited emails and existing user IDs from request
            @SuppressWarnings("unchecked")
            List<String> invitedEmails = (List<String>) groupData.get("invitedEmails");
            @SuppressWarnings("unchecked")
            List<String> existingUserIds = (List<String>) groupData.get("userIds");

            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("➕ CREATE GROUP - Starting");
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("📧 Invited Emails: " + invitedEmails);
            System.out.println("👥 Existing User IDs: " + existingUserIds);

            if (competitionId == null || category == null || groupName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: competitionId, category, and name are required"
                ));
            }

            // Validar que el creatorUserId NO haya creado ya un grupo con este competitionId Y este nombre
            // Validar: creatorUserId + competitionId + name (nombre del grupo)
            Group existingGroup = groupsRepository.findByCreatorUserIdAndCompetitionIdAndName(userId, competitionId, groupName);
            if (existingGroup != null) {
                System.out.println("⚠️ VALIDATION FAILED: User " + userId + " already created a group with name '" + groupName + "' for competition " + competitionId);
                System.out.println("⚠️ Existing group ID: " + existingGroup.getGroupId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "El grupo ya fue creado",
                    "message", "Ya creaste un grupo con este nombre para esta competencia",
                    "existingGroupId", existingGroup.getGroupId(),
                    "existingGroupName", existingGroup.getName(),
                    "existingGroup", existingGroup
                ));
            }

            // Fetch competition details
            Map<String, Object> competition = competitionsClient.getCompetitionWithTeams(category, competitionId, jwtToken);
            if (competition == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Competition not found"
                ));
            }

            // Fetch qualified teams
            List<Map<String, Object>> qualifiedTeams = competitionsClient.getQualifiedTeams(category, competitionId, jwtToken);
            
            // Validate that we have qualified teams
            if (qualifiedTeams == null || qualifiedTeams.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot create group: the competition has no qualified teams",
                    "message", "Please ensure the competition has qualified teams before creating a group"
                ));
            }
            
            System.out.println("✅ Qualified teams fetched: " + qualifiedTeams.size() + " teams");
            
            // Get totalBetAmount from request (required)
            Object totalBetAmountObj = groupData.get("totalBetAmount");
            if (totalBetAmountObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "totalBetAmount is required"
                ));
            }
            Double totalBetAmount;
            if (totalBetAmountObj instanceof Number) {
                totalBetAmount = ((Number) totalBetAmountObj).doubleValue();
            } else if (totalBetAmountObj instanceof String) {
                try {
                    totalBetAmount = Double.parseDouble((String) totalBetAmountObj);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "totalBetAmount must be a valid number"
                    ));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "totalBetAmount must be a number"
                ));
            }
            
            // Validate totalBetAmount is positive
            if (totalBetAmount <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "totalBetAmount must be greater than 0"
                ));
            }
            
            // Validate that totalBetAmount meets minimum requirements
            // Minimum total bet amount is $50 (not per user, but total)
            final double MINIMUM_TOTAL_BET_AMOUNT = 50.0;
            
            if (totalBetAmount < MINIMUM_TOTAL_BET_AMOUNT) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "totalBetAmount must be at least $" + MINIMUM_TOTAL_BET_AMOUNT + " (monto mínimo total del grupo)",
                    "minimumTotalBetAmount", MINIMUM_TOTAL_BET_AMOUNT
                ));
            }
            
            // Calculate paymentDeadline: 15 days after competition start date
            Date competitionStartDate = null;
            Object poolaAvailableDayObj = competition.get("poolaAvailableDay");
            if (poolaAvailableDayObj != null) {
                if (poolaAvailableDayObj instanceof Date) {
                    competitionStartDate = (Date) poolaAvailableDayObj;
                } else if (poolaAvailableDayObj instanceof Long) {
                    competitionStartDate = new Date((Long) poolaAvailableDayObj);
                } else if (poolaAvailableDayObj instanceof String) {
                    // Try to parse string date (ISO format)
                    try {
                        long timestamp = Long.parseLong((String) poolaAvailableDayObj);
                        competitionStartDate = new Date(timestamp);
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ WARNING: Could not parse competition start date: " + poolaAvailableDayObj);
                    }
                }
            }
            
            Date paymentDeadline = null;
            if (competitionStartDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(competitionStartDate);
                cal.add(Calendar.DAY_OF_MONTH, 15); // Add 15 days
                paymentDeadline = cal.getTime();
                System.out.println("📅 Competition start date: " + competitionStartDate);
                System.out.println("📅 Payment deadline (15 days after): " + paymentDeadline);
            } else {
                System.err.println("⚠️ WARNING: Competition start date (poolaAvailableDay) not found. Payment deadline will not be set.");
            }

            // Create group
            Group group = new Group();
            group.setCreatorUserId(userId);
            group.setCompetitionId(competitionId);
            group.setCompetitionName((String) competition.get("name"));
            group.setCompetitionImage((String) competition.get("image"));
            group.setName(groupName); // Set the group name
            
            // Extract team IDs
            List<String> teamIds = new ArrayList<>();
            for (Map<String, Object> team : qualifiedTeams) {
                teamIds.add((String) team.get("id"));
            }
            group.setTeamIds(teamIds);
            
            // Initialize users list with creator
            List<Group.GroupUser> usersList = new ArrayList<>();
            
            // Get creator information from auth_service
            Map<String, Object> creatorInfo = authClientService.getUserById(userId, jwtToken);
            String creatorName = creatorInfo.get("name") != null ? creatorInfo.get("name").toString() : 
                                (creatorInfo.get("firstName") != null ? creatorInfo.get("firstName").toString() : 
                                (userEmail != null ? userEmail.split("@")[0] : "Usuario"));
            
            // Create GroupUser for creator
            Group.GroupUser creatorUser = new Group.GroupUser();
            creatorUser.setId(userId);
            creatorUser.setNombre(creatorName);
            creatorUser.setScore(0);
            usersList.add(creatorUser);
            
            // Add existing users if provided
            if (existingUserIds != null && !existingUserIds.isEmpty()) {
                for (String existingUserId : existingUserIds) {
                    if (!existingUserId.equals(userId)) {
                        // Get user information from auth_service
                        Map<String, Object> existingUserInfo = authClientService.getUserById(existingUserId, jwtToken);
                        String existingUserName = existingUserInfo.get("name") != null ? existingUserInfo.get("name").toString() : 
                                                 (existingUserInfo.get("firstName") != null ? existingUserInfo.get("firstName").toString() : 
                                                 (existingUserInfo.get("email") != null ? ((String) existingUserInfo.get("email")).split("@")[0] : "Usuario"));
                        
                        Group.GroupUser existingUser = new Group.GroupUser();
                        existingUser.setId(existingUserId);
                        existingUser.setNombre(existingUserName);
                        existingUser.setScore(0);
                        usersList.add(existingUser);
                    }
                }
            }
            group.setUsers(usersList);
            
            // Initialize invited emails list
            List<String> invitedEmailsList = new ArrayList<>();
            if (invitedEmails != null && !invitedEmails.isEmpty()) {
                invitedEmailsList.addAll(invitedEmails);
            }
            group.setInvitedEmails(invitedEmailsList);
            
            // Initialize scoreboard
            Group.Scoreboard scoreboard = new Group.Scoreboard();
            List<Group.Scoreboard.TeamScore> teamScores = new ArrayList<>();
            
            for (Map<String, Object> team : qualifiedTeams) {
                Group.Scoreboard.TeamScore teamScore = new Group.Scoreboard.TeamScore();
                teamScore.setTeamId((String) team.get("id"));
                teamScore.setTeamName((String) team.get("name"));
                teamScore.setTeamFlag((String) team.get("flag"));
                teamScore.setPlayed(0);
                teamScore.setWon(0);
                teamScore.setDrawn(0);
                teamScore.setLost(0);
                teamScore.setGoalsFor(0);
                teamScore.setGoalsAgainst(0);
                teamScore.setGoalDifference(0);
                teamScore.setPoints(0);
                teamScores.add(teamScore);
            }
            scoreboard.setTeams(teamScores);
            group.setScoreboard(scoreboard);
            
            // Initialize tournament structure with groups and matches
            Group.TournamentStructure tournamentStructure = initializeTournamentStructure(qualifiedTeams);
            group.setTournamentStructure(tournamentStructure);
            
            // Set betting information
            group.setTotalBetAmount(totalBetAmount);
            group.setPaymentDeadline(paymentDeadline);
            
            // Initialize user payments map with creator
            Map<String, Group.UserPayment> userPayments = new HashMap<>();
            
            // Calculate equitable amount per user
            int currentUserCount = (group.getUsers() != null ? group.getUsers().size() : 0) + (invitedEmails != null ? invitedEmails.size() : 0);
            double equitableAmount = currentUserCount > 0 ? totalBetAmount / currentUserCount : totalBetAmount;
            group.setEquitableAmountPerUser(equitableAmount);
            
            // Create payment record for creator
            Group.UserPayment creatorPayment = new Group.UserPayment();
            creatorPayment.setUserId(userId);
            creatorPayment.setUserEmail(userEmail);
            creatorPayment.setPaymentAmount(equitableAmount);
            creatorPayment.setHasPaid(false);
            creatorPayment.setIsCreator(true);
            creatorPayment.setPaidDate(null);
            userPayments.put(userId, creatorPayment);
            
            // Add payments for existing users if any
            if (existingUserIds != null && !existingUserIds.isEmpty()) {
                for (String existingUserId : existingUserIds) {
                    if (!existingUserId.equals(userId)) {
                        Group.UserPayment userPayment = new Group.UserPayment();
                        userPayment.setUserId(existingUserId);
                        userPayment.setPaymentAmount(equitableAmount);
                        userPayment.setHasPaid(false);
                        userPayment.setIsCreator(false);
                        userPayment.setPaidDate(null);
                        userPayments.put(existingUserId, userPayment);
                    }
                }
            }
            group.setUserPayments(userPayments);
            
            // Set timestamps
            Date now = new Date();
            group.setCreatedAt(now);
            group.setUpdatedAt(now);
            group.setEnabledAt(now);
            
            // Verify tournament structure before saving
            if (group.getTournamentStructure() != null && group.getTournamentStructure().getStages() != null) {
                Group.TournamentStructure.Stage groupStage = group.getTournamentStructure().getStages().get("group-stage");
                if (groupStage != null && groupStage.getGroups() != null) {
                    int totalMatches = 0;
                    for (Group.TournamentStructure.GroupStage g : groupStage.getGroups()) {
                        if (g.getMatches() != null) {
                            totalMatches += g.getMatches().size();
                            System.out.println("   📋 Grupo " + g.getGroupLetter() + " tiene " + g.getMatches().size() + " partidos listos");
                        }
                    }
                    System.out.println("   📊 Total partidos en fase de grupos: " + totalMatches);
                }
            }
            
            // Save group
            Group savedGroup = groupsRepository.save(group);
            System.out.println("✅ Group created with ID: " + savedGroup.getGroupId());
            
            // OBLIGATORIO: Enviar correos a usuarios no registrados (invitedEmails)
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("📧 EMAIL SENDING PROCESS - Starting");
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("📧 Invited Emails received: " + (invitedEmails != null ? invitedEmails.size() : 0));
            
            if (invitedEmails == null || invitedEmails.isEmpty()) {
                System.out.println("⚠️ WARNING: No invitedEmails provided. No emails will be sent.");
            } else {
                System.out.println("📧 Processing " + invitedEmails.size() + " email(s) for users not registered...");
                String groupNameForEmail = savedGroup.getName() != null ? savedGroup.getName() : ("Group for " + savedGroup.getCompetitionName());
                
                int successCount = 0;
                int failureCount = 0;
                List<String> failedEmails = new ArrayList<>();
                
                for (String email : invitedEmails) {
                    if (email == null || email.trim().isEmpty()) {
                        System.err.println("⚠️ Skipping empty email in invitedEmails list");
                        continue;
                    }
                    
                    try {
                        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        System.out.println("📧 Processing email #" + (successCount + failureCount + 1) + "/" + invitedEmails.size());
                        System.out.println("📧 Email: " + email);
                        System.out.println("📧 Group ID: " + savedGroup.getGroupId());
                        System.out.println("📧 Group Name: " + groupNameForEmail);
                        
                        boolean emailSent = emailService.sendGroupInvitation(
                            email.trim(),
                            savedGroup.getGroupId(),
                            groupNameForEmail,
                            userEmail,
                            savedGroup.getCompetitionName()
                        );
                        
                        if (emailSent) {
                            successCount++;
                            System.out.println("✅ SUCCESS: Email sent to: " + email);
                        } else {
                            failureCount++;
                            failedEmails.add(email);
                            System.err.println("❌ FAILED: Could not send email to: " + email);
                        }
                    } catch (Exception e) {
                        failureCount++;
                        failedEmails.add(email);
                        System.err.println("❌ EXCEPTION sending email to " + email + ":");
                        System.err.println("   Error: " + e.getClass().getName());
                        System.err.println("   Message: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                System.out.println("═══════════════════════════════════════════════════════");
                System.out.println("📧 EMAIL SENDING SUMMARY:");
                System.out.println("   Total emails to send: " + invitedEmails.size());
                System.out.println("   ✅ Sent successfully: " + successCount);
                System.out.println("   ❌ Failed: " + failureCount);
                if (!failedEmails.isEmpty()) {
                    System.err.println("   Failed emails: " + failedEmails);
                }
                System.out.println("═══════════════════════════════════════════════════════");
                
                // Avisar si hubo fallos
                if (failureCount > 0) {
                    System.err.println("⚠️ WARNING: " + failureCount + " email(s) failed to send. Check logs above for details.");
                }
            }

            // Update users in auth_service with this new groupId (creator + existing members)
            try {
                String groupId = savedGroup.getGroupId();
                System.out.println("🔗 Updating users with groupId in auth_service...");
                // Creator
                authClientService.addGroupToUser(userId, groupId, jwtToken);
                // Existing users (if any)
                if (existingUserIds != null && !existingUserIds.isEmpty()) {
                    for (String existingUserId : existingUserIds) {
                        if (existingUserId != null && !existingUserId.isEmpty()) {
                            authClientService.addGroupToUser(existingUserId, groupId, jwtToken);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ WARNING: Failed to update user groups in auth_service: " + e.getMessage());
            }
            
            // Normalize group before returning
            savedGroup = normalizeGroup(savedGroup);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Group created successfully",
                "group", savedGroup,
                "invitedEmails", invitedEmails != null ? invitedEmails.size() : 0,
                "addedUserIds", existingUserIds != null ? existingUserIds.size() : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error creating group: " + e.getMessage()
            ));
        }
    }

    /**
     * Get group by ID
     */
    public ResponseEntity<Map<String, Object>> getGroupById(String groupId, String userId) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();
            
            // Normalize group after reading from MongoDB
            group = normalizeGroup(group);
            
            // Check if user has access to this group
            if (!isUserInGroup(group, userId) &&
                !group.getInvitedEmails().contains(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have access to this group"
                ));
            }

            return ResponseEntity.ok(Map.of("group", group));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving group: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all groups for a user (as creator or member)
     */
    public ResponseEntity<Map<String, Object>> getUserGroups(String userId) {
        try {
            List<Group> groups = groupsRepository.findGroupsByUserIdAsCreatorOrMember(userId);
            // Normalize all groups after reading from MongoDB
            if (groups != null) {
                groups = groups.stream()
                    .map(this::normalizeGroup)
                    .toList();
            }
            return ResponseEntity.ok(Map.of(
                "groups", groups,
                "count", groups != null ? groups.size() : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving groups: " + e.getMessage()
            ));
        }
    }

    /**
     * Update group (PUT - full update)
     */
    public ResponseEntity<Map<String, Object>> updateGroup(String groupId, String userId, Map<String, Object> updates) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();
            
            // Only creator can update group
            if (!group.getCreatorUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Only the group creator can update the group"
                ));
            }

            // Update fields (preserve critical fields)
            if (updates.containsKey("users")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> usersData = (List<Map<String, Object>>) updates.get("users");
                if (usersData != null) {
                    List<Group.GroupUser> usersList = new ArrayList<>();
                    for (Map<String, Object> userData : usersData) {
                        Group.GroupUser user = new Group.GroupUser();
                        user.setId((String) userData.get("id"));
                        user.setNombre((String) userData.get("nombre"));
                        Object scoreObj = userData.get("score");
                        user.setScore(scoreObj != null ? (scoreObj instanceof Integer ? (Integer) scoreObj : ((Number) scoreObj).intValue()) : 0);
                        usersList.add(user);
                    }
                    group.setUsers(usersList);
                }
            }
            if (updates.containsKey("invitedEmails")) {
                group.setInvitedEmails((List<String>) updates.get("invitedEmails"));
            }
            if (updates.containsKey("enabledAt")) {
                Object enabledAtObj = updates.get("enabledAt");
                if (enabledAtObj != null) {
                    group.setEnabledAt(enabledAtObj instanceof Date ? (Date) enabledAtObj : new Date((Long) enabledAtObj));
                }
            }
            if (updates.containsKey("disabledAt")) {
                Object disabledAtObj = updates.get("disabledAt");
                if (disabledAtObj != null) {
                    group.setDisabledAt(disabledAtObj instanceof Date ? (Date) disabledAtObj : new Date((Long) disabledAtObj));
                }
            }
            if (updates.containsKey("totalBetAmount")) {
                Object totalBetAmountObj = updates.get("totalBetAmount");
                if (totalBetAmountObj != null) {
                    Double amount;
                    if (totalBetAmountObj instanceof Number) {
                        amount = ((Number) totalBetAmountObj).doubleValue();
                    } else if (totalBetAmountObj instanceof String) {
                        try {
                            amount = Double.parseDouble((String) totalBetAmountObj);
                        } catch (NumberFormatException e) {
                            return ResponseEntity.badRequest().body(Map.of(
                                "error", "totalBetAmount must be a valid number"
                            ));
                        }
                    } else {
                        return ResponseEntity.badRequest().body(Map.of(
                            "error", "totalBetAmount must be a number"
                        ));
                    }
                    if (amount > 0) {
                        // Validate that totalBetAmount meets minimum requirements
                        // Minimum total bet amount is $50 (not per user, but total)
                        final double MINIMUM_TOTAL_BET_AMOUNT = 50.0;
                        
                        if (amount < MINIMUM_TOTAL_BET_AMOUNT) {
                            return ResponseEntity.badRequest().body(Map.of(
                                "error", "totalBetAmount must be at least $" + MINIMUM_TOTAL_BET_AMOUNT + " (monto mínimo total del grupo)",
                                "minimumTotalBetAmount", MINIMUM_TOTAL_BET_AMOUNT
                            ));
                        }
                        
                        group.setTotalBetAmount(amount);
                        // Recalculate equitable amount per user and update all user payments
                        recalculateEquitableAmount(group);
                    } else {
                        return ResponseEntity.badRequest().body(Map.of(
                            "error", "totalBetAmount must be greater than 0"
                        ));
                    }
                }
            }
            if (updates.containsKey("paymentDeadline")) {
                Object paymentDeadlineObj = updates.get("paymentDeadline");
                if (paymentDeadlineObj != null) {
                    Date deadline;
                    if (paymentDeadlineObj instanceof Date) {
                        deadline = (Date) paymentDeadlineObj;
                    } else if (paymentDeadlineObj instanceof Long) {
                        deadline = new Date((Long) paymentDeadlineObj);
                    } else if (paymentDeadlineObj instanceof String) {
                        try {
                            long timestamp = Long.parseLong((String) paymentDeadlineObj);
                            deadline = new Date(timestamp);
                        } catch (NumberFormatException e) {
                            return ResponseEntity.badRequest().body(Map.of(
                                "error", "paymentDeadline must be a valid date timestamp"
                            ));
                        }
                    } else {
                        return ResponseEntity.badRequest().body(Map.of(
                            "error", "paymentDeadline must be a date"
                        ));
                    }
                    group.setPaymentDeadline(deadline);
                }
            }

            group.setUpdatedAt(new Date());
            Group updatedGroup = groupsRepository.save(group);

            return ResponseEntity.ok(Map.of(
                "message", "Group updated successfully",
                "group", updatedGroup
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error updating group: " + e.getMessage()
            ));
        }
    }

    /**
     * Patch group (PATCH - partial update)
     */
    public ResponseEntity<Map<String, Object>> patchGroup(String groupId, String userId, Map<String, Object> updates) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();
            
            // Only creator can patch group
            if (!group.getCreatorUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Only the group creator can update the group"
                ));
            }

            // Apply partial updates
            updates.forEach((key, value) -> {
                switch (key) {
                    case "userIds" -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> usersData = (List<Map<String, Object>>) value;
                        if (usersData != null) {
                            List<Group.GroupUser> usersList = new ArrayList<>();
                            for (Map<String, Object> userData : usersData) {
                                Group.GroupUser user = new Group.GroupUser();
                                user.setId((String) userData.get("id"));
                                user.setNombre((String) userData.get("nombre"));
                                Object scoreObj = userData.get("score");
                                user.setScore(scoreObj != null ? (scoreObj instanceof Integer ? (Integer) scoreObj : ((Number) scoreObj).intValue()) : 0);
                                usersList.add(user);
                            }
                            group.setUsers(usersList);
                        }
                        // Recalculate equitable amount when users change
                        recalculateEquitableAmount(group);
                    }
                    case "invitedEmails" -> {
                        group.setInvitedEmails((List<String>) value);
                        // Recalculate equitable amount when invited emails change
                        recalculateEquitableAmount(group);
                    }
                    case "disabledAt" -> group.setDisabledAt(value != null ? new Date((Long) value) : null);
                    case "totalBetAmount" -> {
                        if (value != null) {
                            Double amount;
                            if (value instanceof Number) {
                                amount = ((Number) value).doubleValue();
                            } else if (value instanceof String) {
                                try {
                                    amount = Double.parseDouble((String) value);
                                } catch (NumberFormatException e) {
                                    System.err.println("⚠️ Invalid totalBetAmount format: " + value);
                                    return;
                                }
                            } else {
                                return;
                            }
                            if (amount > 0) {
                                // Validate that totalBetAmount meets minimum requirements
                                // Minimum total bet amount is $50 (not per user, but total)
                                final double MINIMUM_TOTAL_BET_AMOUNT = 50.0;
                                
                                if (amount < MINIMUM_TOTAL_BET_AMOUNT) {
                                    System.err.println("⚠️ totalBetAmount must be at least $" + MINIMUM_TOTAL_BET_AMOUNT + " (monto mínimo total del grupo)");
                                    return;
                                }
                                
                                group.setTotalBetAmount(amount);
                                // Recalculate equitable amount per user and update all user payments
                                recalculateEquitableAmount(group);
                            }
                        }
                    }
                    case "paymentDeadline" -> {
                        if (value != null) {
                            if (value instanceof Date) {
                                group.setPaymentDeadline((Date) value);
                            } else if (value instanceof Long) {
                                group.setPaymentDeadline(new Date((Long) value));
                            } else if (value instanceof String) {
                                try {
                                    long timestamp = Long.parseLong((String) value);
                                    group.setPaymentDeadline(new Date(timestamp));
                                } catch (NumberFormatException e) {
                                    System.err.println("⚠️ Invalid paymentDeadline format: " + value);
                                }
                            }
                        }
                    }
                }
            });

            group.setUpdatedAt(new Date());
            Group patchedGroup = groupsRepository.save(group);

            return ResponseEntity.ok(Map.of(
                "message", "Group patched successfully",
                "group", patchedGroup
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error patching group: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete group
     */
    public ResponseEntity<Map<String, Object>> deleteGroup(String groupId, String userId) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();
            
            // Only creator can delete group
            if (!group.getCreatorUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Only the group creator can delete the group"
                ));
            }

            groupsRepository.deleteById(groupId);

            return ResponseEntity.ok(Map.of(
                "message", "Group deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error deleting group: " + e.getMessage()
            ));
        }
    }

    /**
     * Send invitation to join group
     */
    public ResponseEntity<Map<String, Object>> inviteToGroup(String groupId, String userId, String userEmail, Map<String, Object> inviteData) {
        try {
            System.out.println("📧 inviteToGroup called - Group ID: " + groupId);
            System.out.println("📧 User ID: " + userId);
            System.out.println("📧 User Email: " + userEmail);
            System.out.println("📧 Invite Data: " + inviteData);

            String inviteeEmail = (String) inviteData.get("email");
            if (inviteeEmail == null || inviteeEmail.trim().isEmpty()) {
                System.err.println("❌ Email is required");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Email is required"
                ));
            }

            System.out.println("📧 Invitee Email: " + inviteeEmail);

            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                System.err.println("❌ Group not found: " + groupId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();
            System.out.println("📧 Group found: " + group.getCompetitionName());
            
            // Check if user has permission to invite (creator or member)
            if (!isUserInGroup(group, userId)) {
                System.err.println("❌ User doesn't have permission to invite");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have permission to invite users to this group"
                ));
            }

            // Check if email is already invited
            if (group.getInvitedEmails().contains(inviteeEmail)) {
                System.err.println("❌ Email already invited: " + inviteeEmail);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "This email has already been invited"
                ));
            }

            // Add email to invited list
            group.getInvitedEmails().add(inviteeEmail);
            group.setUpdatedAt(new Date());
            Group savedGroup = groupsRepository.save(group);
            System.out.println("✅ Email added to invited list: " + inviteeEmail);

            // Send invitation email
            String groupName = group.getName() != null ? group.getName() : ("Group for " + group.getCompetitionName());
            System.out.println("📧 Sending invitation email to: " + inviteeEmail);
            System.out.println("📧 Group name: " + groupName);
            
            boolean emailSent = emailService.sendGroupInvitation(
                inviteeEmail,
                groupId,
                groupName,
                userEmail,
                group.getCompetitionName()
            );

            if (!emailSent) {
                System.err.println("❌ Failed to send invitation email to: " + inviteeEmail);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to send invitation email",
                    "details", "Email was added to invited list but sending failed. Please check server logs."
                ));
            }

            System.out.println("✅ Invitation email sent successfully to: " + inviteeEmail);
            return ResponseEntity.ok(Map.of(
                "message", "Invitation sent successfully",
                "invitedEmail", inviteeEmail
            ));
        } catch (Exception e) {
            System.err.println("❌ Exception in inviteToGroup: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error sending invitation: " + e.getMessage()
            ));
        }
    }

    /**
     * Join a group (for invited users)
     */
    public ResponseEntity<Map<String, Object>> joinGroup(String groupId, String userId, String userEmail, String jwtToken) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();

            // Check if user is already a member
            if (isUserInGroup(group, userId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "You are already a member of this group"
                ));
            }

            // Check if user was invited
            if (!group.getInvitedEmails().contains(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have an invitation to join this group"
                ));
            }

            // Get user information from auth_service
            Map<String, Object> userInfo = authClientService.getUserById(userId, jwtToken);
            String userName = userInfo.get("name") != null ? userInfo.get("name").toString() : 
                            (userInfo.get("firstName") != null ? userInfo.get("firstName").toString() : 
                            (userInfo.get("email") != null ? ((String) userInfo.get("email")).split("@")[0] : "Usuario"));
            
            // Add user to group and remove from invited emails
            addUserToGroup(group, userId, userName, jwtToken);
            group.getInvitedEmails().remove(userEmail);
            
            // Initialize user payments map if null
            if (group.getUserPayments() == null) {
                group.setUserPayments(new HashMap<>());
            }
            
            // Recalculate equitable amount per user (includes new member)
            recalculateEquitableAmount(group);
            
            // Create payment record for new member
            if (!group.getUserPayments().containsKey(userId)) {
                Group.UserPayment userPayment = new Group.UserPayment();
                userPayment.setUserId(userId);
                userPayment.setUserEmail(userEmail);
                userPayment.setPaymentAmount(group.getEquitableAmountPerUser()); // Equal share
                userPayment.setHasPaid(false);
                userPayment.setIsCreator(false);
                userPayment.setPaidDate(null);
                group.getUserPayments().put(userId, userPayment);
            } else {
                // Update existing payment amount to new equitable amount
                Group.UserPayment existingPayment = group.getUserPayments().get(userId);
                existingPayment.setPaymentAmount(group.getEquitableAmountPerUser());
            }
            
            group.setUpdatedAt(new Date());
            Group updatedGroup = groupsRepository.save(group);

            // Update user's groups in auth_service
            try {
                authClientService.addGroupToUser(userId, groupId, jwtToken);
            } catch (Exception e) {
                System.err.println("⚠️ WARNING: Failed to update user group in auth_service on join: " + e.getMessage());
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Successfully joined the group",
                "group", updatedGroup
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error joining group: " + e.getMessage()
            ));
        }
    }

    /**
     * Validate emails - check if users exist in the system
     */
    public ResponseEntity<Map<String, Object>> validateEmails(List<String> emails, String jwtToken) {
        try {
            if (emails == null || emails.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Emails list is required"
                ));
            }

            Map<String, Object> validation = userValidationService.validateEmails(emails, jwtToken);
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error validating emails: " + e.getMessage()
            ));
        }
    }

    /**
     * Test email - for debugging
     */
    public ResponseEntity<Map<String, Object>> testEmail(String toEmail) {
        try {
            System.out.println("🧪 Testing email to: " + toEmail);
            
            boolean emailSent = emailService.sendGroupInvitation(
                toEmail,
                "test-group-id",
                "Grupo de Prueba",
                "Test User",
                "Competencia de Prueba"
            );
            
            if (emailSent) {
                return ResponseEntity.ok(Map.of(
                    "message", "Test email sent successfully",
                    "email", toEmail
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to send test email",
                    "email", toEmail
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error sending test email: " + e.getMessage()
            ));
        }
    }

    /**
     * Find all groups where the given email is in invitedEmails.
     * Used by auth_service when a user registers to know which groups had invited them.
     */
    public ResponseEntity<Map<String, Object>> getGroupsByInvitedEmail(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Email is required"
                ));
            }

            List<Group> groups = groupsRepository.findByInvitedEmailsContaining(email);
            List<String> groupIds = groups.stream()
                    .map(Group::getGroupId)
                    .toList();

            return ResponseEntity.ok(Map.of(
                "email", email,
                "groupIds", groupIds,
                "count", groupIds.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving groups by invited email: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all matches for a group with optional filters
     */
    public ResponseEntity<Map<String, Object>> getMatches(String groupId, String userId, String stageId, String groupLetter, String status) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();
            
            // Verify user has access to this group
            if (!isUserInGroup(group, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have access to this group"
                ));
            }

            List<Group.TournamentStructure.Match> allMatches = new ArrayList<>();
            
            System.out.println("🔍 Getting matches for group: " + groupId);
            System.out.println("   Tournament structure exists: " + (group.getTournamentStructure() != null));
            
            if (group.getTournamentStructure() != null && group.getTournamentStructure().getStages() != null) {
                System.out.println("   Number of stages: " + group.getTournamentStructure().getStages().size());
                
                for (Group.TournamentStructure.Stage stage : group.getTournamentStructure().getStages().values()) {
                    // Filter by stageId if provided
                    if (stageId != null && !stage.getStageId().equals(stageId)) {
                        continue;
                    }

                    System.out.println("   Processing stage: " + stage.getStageId() + " (type: " + stage.getType() + ")");

                    // Collect matches from group stage
                    if (stage.getType().equals("groups") && stage.getGroups() != null) {
                        System.out.println("   Number of groups in stage: " + stage.getGroups().size());
                        for (Group.TournamentStructure.GroupStage groupStage : stage.getGroups()) {
                            // Filter by groupLetter if provided
                            if (groupLetter != null && !groupStage.getGroupLetter().equals(groupLetter)) {
                                continue;
                            }
                            
                            System.out.println("   Group " + groupStage.getGroupLetter() + " has " + 
                                (groupStage.getMatches() != null ? groupStage.getMatches().size() : 0) + " matches");
                            
                            if (groupStage.getMatches() != null) {
                                for (Group.TournamentStructure.Match match : groupStage.getMatches()) {
                                    // Filter by status if provided
                                    if (status == null || (match.getStatus() != null && match.getStatus().equals(status))) {
                                        allMatches.add(match);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Collect matches from knockout stages
                    if (stage.getType().equals("knockout") && stage.getMatches() != null) {
                        System.out.println("   Knockout stage has " + stage.getMatches().size() + " matches");
                        for (Group.TournamentStructure.Match match : stage.getMatches()) {
                            // Filter by status if provided
                            if (status == null || (match.getStatus() != null && match.getStatus().equals(status))) {
                                allMatches.add(match);
                            }
                        }
                    }
                }
            } else {
                System.err.println("⚠️ WARNING: Tournament structure is null or stages map is null!");
            }
            
            System.out.println("   Total matches found: " + allMatches.size());

            return ResponseEntity.ok(Map.of(
                "groupId", groupId,
                "matches", allMatches,
                "count", allMatches.size(),
                "filters", Map.of(
                    "stageId", stageId != null ? stageId : "all",
                    "groupLetter", groupLetter != null ? groupLetter : "all",
                    "status", status != null ? status : "all"
                )
            ));
        } catch (Exception e) {
            System.err.println("❌ Error getting matches: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error getting matches: " + e.getMessage()
            ));
        }
    }

    /**
     * Get a specific match by ID
     */
    public ResponseEntity<Map<String, Object>> getMatchById(String groupId, String matchId, String userId) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();
            
            // Verify user has access
            if (!isUserInGroup(group, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have access to this group"
                ));
            }

            Group.TournamentStructure.Match foundMatch = null;
            
            if (group.getTournamentStructure() != null && group.getTournamentStructure().getStages() != null) {
                for (Group.TournamentStructure.Stage stage : group.getTournamentStructure().getStages().values()) {
                    // Search in group stage matches
                    if (stage.getGroups() != null) {
                        for (Group.TournamentStructure.GroupStage groupStage : stage.getGroups()) {
                            if (groupStage.getMatches() != null) {
                                for (Group.TournamentStructure.Match match : groupStage.getMatches()) {
                                    if (match.getMatchId().equals(matchId)) {
                                        foundMatch = match;
                                        break;
                                    }
                                }
                            }
                            if (foundMatch != null) break;
                        }
                    }
                    
                    // Search in knockout matches
                    if (stage.getMatches() != null) {
                        for (Group.TournamentStructure.Match match : stage.getMatches()) {
                            if (match.getMatchId().equals(matchId)) {
                                foundMatch = match;
                                break;
                            }
                        }
                    }
                    
                    if (foundMatch != null) break;
                }
            }

            if (foundMatch == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Match not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "groupId", groupId,
                "match", foundMatch
            ));
        } catch (Exception e) {
            System.err.println("❌ Error getting match: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error getting match: " + e.getMessage()
            ));
        }
    }

    /**
     * Register or update match result
     * This will automatically update team statistics
     * NOTE: This endpoint is NOT available for frontend use.
     * Results must be registered by backend/admin only.
     */
    public ResponseEntity<Map<String, Object>> registerMatchResult(String groupId, String matchId, String userId, Map<String, Object> resultData) {
        // Frontend cannot register real match results
        // Results must be registered by backend/admin only
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "Frontend cannot register match results. Results must be registered by backend/admin only."
        ));
    }

    /**
     * Clear match result (reset to not played)
     * NOTE: This endpoint is NOT available for frontend use.
     * Results must be cleared by backend/admin only.
     */
    public ResponseEntity<Map<String, Object>> clearMatchResult(String groupId, String matchId, String userId) {
        // Frontend cannot clear real match results
        // Results must be cleared by backend/admin only
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "Frontend cannot clear match results. Results must be managed by backend/admin only."
        ));
    }

    private void revertTeamStats(List<Group.TeamScore> teams, String team1Id, String team2Id, Integer team1Score, Integer team2Score) {
        for (Group.TeamScore team : teams) {
            if (team.getTeamId().equals(team1Id)) {
                team.setPlayed(Math.max(0, team.getPlayed() - 1));
                team.setGoalsFor(Math.max(0, team.getGoalsFor() - team1Score));
                team.setGoalsAgainst(Math.max(0, team.getGoalsAgainst() - team2Score));
                team.setGoalDifference(team.getGoalsFor() - team.getGoalsAgainst());
                
                if (team1Score > team2Score) {
                    team.setWon(Math.max(0, team.getWon() - 1));
                    team.setPoints(Math.max(0, team.getPoints() - 3));
                } else if (team1Score < team2Score) {
                    team.setLost(Math.max(0, team.getLost() - 1));
                } else {
                    team.setDrawn(Math.max(0, team.getDrawn() - 1));
                    team.setPoints(Math.max(0, team.getPoints() - 1));
                }
            } else if (team.getTeamId().equals(team2Id)) {
                team.setPlayed(Math.max(0, team.getPlayed() - 1));
                team.setGoalsFor(Math.max(0, team.getGoalsFor() - team2Score));
                team.setGoalsAgainst(Math.max(0, team.getGoalsAgainst() - team1Score));
                team.setGoalDifference(team.getGoalsFor() - team.getGoalsAgainst());
                
                if (team2Score > team1Score) {
                    team.setWon(Math.max(0, team.getWon() - 1));
                    team.setPoints(Math.max(0, team.getPoints() - 3));
                } else if (team2Score < team1Score) {
                    team.setLost(Math.max(0, team.getLost() - 1));
                } else {
                    team.setDrawn(Math.max(0, team.getDrawn() - 1));
                    team.setPoints(Math.max(0, team.getPoints() - 1));
                }
            }
        }
    }

    private void removeWinnerFromNextMatch(Group group, String nextMatchId, String nextStageId, String winnerTeamId) {
        if (group.getTournamentStructure() != null && group.getTournamentStructure().getStages() != null) {
            Group.TournamentStructure.Stage nextStage = group.getTournamentStructure().getStages().get(nextStageId);
            if (nextStage != null && nextStage.getMatches() != null) {
                for (Group.TournamentStructure.Match nextMatch : nextStage.getMatches()) {
                    if (nextMatch.getMatchId().equals(nextMatchId)) {
                        if (nextMatch.getTeam1Id() != null && nextMatch.getTeam1Id().equals(winnerTeamId)) {
                            nextMatch.setTeam1Id(null);
                            nextMatch.setTeam1Name(null);
                            nextMatch.setTeam1Flag(null);
                        } else if (nextMatch.getTeam2Id() != null && nextMatch.getTeam2Id().equals(winnerTeamId)) {
                            nextMatch.setTeam2Id(null);
                            nextMatch.setTeam2Name(null);
                            nextMatch.setTeam2Flag(null);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Initialize tournament structure with groups and matches
     */
    private Group.TournamentStructure initializeTournamentStructure(List<Map<String, Object>> qualifiedTeams) {
        // Validate that we have teams
        if (qualifiedTeams == null || qualifiedTeams.isEmpty()) {
            throw new IllegalArgumentException("Cannot initialize tournament structure: no qualified teams provided");
        }
        
        Group.TournamentStructure tournamentStructure = new Group.TournamentStructure();
        tournamentStructure.setTournamentFormat("groups-then-knockout");
        tournamentStructure.setCurrentStage("group-stage");
        
        // Configuration
        Group.TournamentStructure.TournamentConfig config = new Group.TournamentStructure.TournamentConfig();
        int totalTeams = qualifiedTeams.size();
        int teamsPerGroup = 4; // Standard: 4 teams per group
        
        // Validate teamsPerGroup to avoid division by zero
        if (teamsPerGroup <= 0) {
            throw new IllegalArgumentException("teamsPerGroup must be greater than 0");
        }
        
        int numberOfGroups = (int) Math.ceil((double) totalTeams / teamsPerGroup);
        
        // Validate numberOfGroups
        if (numberOfGroups <= 0) {
            throw new IllegalArgumentException("Cannot create tournament: numberOfGroups is 0. Total teams: " + totalTeams);
        }
        
        int teamsQualifyPerGroup = 2; // Top 2 qualify
        
        config.setTotalTeams(totalTeams);
        config.setNumberOfGroups(numberOfGroups);
        config.setTeamsPerGroup(teamsPerGroup);
        config.setTeamsQualifyPerGroup(teamsQualifyPerGroup);
        config.setHasGroupStage(true);
        config.setHasKnockoutStage(true);
        config.setKnockoutRounds(List.of("round-of-16", "quarter-finals", "semi-finals", "third-place", "final"));
        tournamentStructure.setConfig(config);
        
        // Initialize stages map
        Map<String, Group.TournamentStructure.Stage> stages = new HashMap<>();
        
        // ========== GROUP STAGE ==========
        Group.TournamentStructure.Stage groupStage = new Group.TournamentStructure.Stage();
        groupStage.setStageId("group-stage");
        groupStage.setStageName("Fase de Grupos");
        groupStage.setType("groups");
        groupStage.setIsActive(true);
        groupStage.setIsCompleted(false);
        groupStage.setOrder(1);
        
        List<Group.TournamentStructure.GroupStage> groups = new ArrayList<>();
        String[] groupLetters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
        
        // Divide teams into groups
        for (int g = 0; g < numberOfGroups; g++) {
            Group.TournamentStructure.GroupStage group = new Group.TournamentStructure.GroupStage();
            group.setGroupLetter(groupLetters[g]);
            group.setGroupName("Grupo " + groupLetters[g]);
            group.setTeamsPerGroup(teamsPerGroup);
            group.setTeamsQualify(teamsQualifyPerGroup);
            
            // Get teams for this group
            List<Group.TeamScore> groupTeams = new ArrayList<>();
            List<Group.TournamentStructure.Match> groupMatches = new ArrayList<>();
            
            int startIdx = g * teamsPerGroup;
            int endIdx = Math.min(startIdx + teamsPerGroup, totalTeams);
            
            for (int i = startIdx; i < endIdx; i++) {
                Map<String, Object> teamData = qualifiedTeams.get(i);
                Group.TeamScore teamScore = new Group.TeamScore();
                teamScore.setTeamId((String) teamData.get("id"));
                teamScore.setTeamName((String) teamData.get("name"));
                teamScore.setTeamFlag((String) teamData.get("flag"));
                teamScore.setPlayed(0);
                teamScore.setWon(0);
                teamScore.setDrawn(0);
                teamScore.setLost(0);
                teamScore.setGoalsFor(0);
                teamScore.setGoalsAgainst(0);
                teamScore.setGoalDifference(0);
                teamScore.setPoints(0);
                teamScore.setPosition(0);
                groupTeams.add(teamScore);
            }
            
            group.setTeams(groupTeams);
            group.setQualifiedTeamIds(new ArrayList<>());
            
            // Generate matches for this group (round-robin: each team plays against all others)
            // For 4 teams: 6 matches total (A vs B, A vs C, A vs D, B vs C, B vs D, C vs D)
            int matchNumber = 1;
            int totalMatchesInGroup = (groupTeams.size() * (groupTeams.size() - 1)) / 2;
            int matchesPerMatchday = 2; // 2 matches per matchday for better scheduling
            int currentMatchday = 1;
            
            for (int i = 0; i < groupTeams.size(); i++) {
                for (int j = i + 1; j < groupTeams.size(); j++) {
                    Group.TournamentStructure.Match match = new Group.TournamentStructure.Match();
                    match.setMatchId("group-" + groupLetters[g].toLowerCase() + "-match-" + matchNumber);
                    match.setMatchNumber(String.valueOf(matchNumber));
                    match.setStageId("group-stage");
                    match.setGroupLetter(groupLetters[g]);
                    match.setTeam1Id(groupTeams.get(i).getTeamId());
                    match.setTeam1Name(groupTeams.get(i).getTeamName());
                    match.setTeam1Flag(groupTeams.get(i).getTeamFlag());
                    match.setTeam2Id(groupTeams.get(j).getTeamId());
                    match.setTeam2Name(groupTeams.get(j).getTeamName());
                    match.setTeam2Flag(groupTeams.get(j).getTeamFlag());
                    match.setTeam1Score(null);
                    match.setTeam2Score(null);
                    match.setWinnerTeamId(null);
                    match.setLoserTeamId(null);
                    match.setIsDraw(null);
                    match.setMatchDate(null);
                    match.setPlayedDate(null);
                    match.setIsPlayed(false);
                    match.setVenue(null);
                    match.setStatus("scheduled");
                    match.setNextMatchId(null);
                    match.setNextStageId(null);
                    match.setMatchday(currentMatchday);
                    
                    groupMatches.add(match);
                    matchNumber++;
                    
                    // Distribute matches across matchdays (2 matches per matchday for 4-team groups)
                    if (matchNumber % matchesPerMatchday == 1 && matchNumber <= totalMatchesInGroup) {
                        currentMatchday++;
                    }
                }
            }
            
            group.setMatches(groupMatches);
            System.out.println("   ✅ Grupo " + groupLetters[g] + ": " + groupMatches.size() + " partidos creados");
            groups.add(group);
        }
        
        groupStage.setGroups(groups);
        groupStage.setQualifiedTeamIds(new ArrayList<>());
        stages.put("group-stage", groupStage);
        
        // ========== KNOCKOUT STAGES ==========
        // Round of 16 (if we have 16+ teams qualifying)
        int teamsInRoundOf16 = numberOfGroups * teamsQualifyPerGroup;
        if (teamsInRoundOf16 >= 16) {
            Group.TournamentStructure.Stage roundOf16 = createKnockoutStage("round-of-16", "Octavos de Final", 2, teamsInRoundOf16);
            stages.put("round-of-16", roundOf16);
        }
        
        // Quarter-finals (8 teams)
        Group.TournamentStructure.Stage quarterFinals = createKnockoutStage("quarter-finals", "Cuartos de Final", 3, 8);
        stages.put("quarter-finals", quarterFinals);
        
        // Semi-finals (4 teams)
        Group.TournamentStructure.Stage semiFinals = createKnockoutStage("semi-finals", "Semifinales", 4, 4);
        stages.put("semi-finals", semiFinals);
        
        // Third place (2 teams)
        Group.TournamentStructure.Stage thirdPlace = createKnockoutStage("third-place", "Tercer Lugar", 5, 2);
        stages.put("third-place", thirdPlace);
        
        // Final (2 teams)
        Group.TournamentStructure.Stage finalStage = createKnockoutStage("final", "Final", 6, 2);
        stages.put("final", finalStage);
        
        tournamentStructure.setStages(stages);
        
        // Count total matches created
        int totalGroupMatches = 0;
        if (groupStage.getGroups() != null) {
            for (Group.TournamentStructure.GroupStage g : groupStage.getGroups()) {
                if (g.getMatches() != null) {
                    totalGroupMatches += g.getMatches().size();
                }
            }
        }
        
        System.out.println("✅ Tournament structure initialized:");
        System.out.println("   Total teams: " + totalTeams);
        System.out.println("   Number of groups: " + numberOfGroups);
        System.out.println("   Teams per group: " + teamsPerGroup);
        System.out.println("   Teams qualify per group: " + teamsQualifyPerGroup);
        System.out.println("   Total matches in group stage: " + totalGroupMatches);
        // Avoid division by zero
        if (numberOfGroups > 0) {
            System.out.println("   Matches per group: " + (totalGroupMatches / numberOfGroups));
        } else {
            System.out.println("   Matches per group: N/A (no groups created)");
        }
        
        return tournamentStructure;
    }
    
    /**
     * Create a knockout stage with empty matches
     */
    private Group.TournamentStructure.Stage createKnockoutStage(String stageId, String stageName, int order, int numberOfMatches) {
        Group.TournamentStructure.Stage stage = new Group.TournamentStructure.Stage();
        stage.setStageId(stageId);
        stage.setStageName(stageName);
        stage.setType("knockout");
        stage.setIsActive(false);
        stage.setIsCompleted(false);
        stage.setOrder(order);
        
        List<Group.TournamentStructure.Match> matches = new ArrayList<>();
        for (int i = 1; i <= numberOfMatches; i++) {
            Group.TournamentStructure.Match match = new Group.TournamentStructure.Match();
            match.setMatchId(stageId + "-" + i);
            match.setMatchNumber(String.valueOf(i));
            match.setStageId(stageId);
            match.setGroupLetter(null);
            match.setTeam1Id(null);
            match.setTeam1Name(null);
            match.setTeam1Flag(null);
            match.setTeam2Id(null);
            match.setTeam2Name(null);
            match.setTeam2Flag(null);
            match.setTeam1Score(null);
            match.setTeam2Score(null);
            match.setWinnerTeamId(null);
            match.setLoserTeamId(null);
            match.setIsDraw(null);
            match.setMatchDate(null);
            match.setPlayedDate(null);
            match.setIsPlayed(false);
            match.setVenue(null);
            match.setStatus("scheduled");
            
            // Set next match ID based on bracket structure
            if (!stageId.equals("final") && !stageId.equals("third-place")) {
                String nextStageId = getNextStageId(stageId);
                int nextMatchNumber = (int) Math.ceil((double) i / 2);
                match.setNextMatchId(nextStageId + "-" + nextMatchNumber);
                match.setNextStageId(nextStageId);
            } else {
                match.setNextMatchId(null);
                match.setNextStageId(null);
            }
            
            match.setMatchday(null);
            matches.add(match);
        }
        
        stage.setMatches(matches);
        stage.setQualifiedTeamIds(new ArrayList<>());
        
        return stage;
    }
    
    /**
     * Get next stage ID based on current stage
     */
    private String getNextStageId(String currentStageId) {
        return switch (currentStageId) {
            case "round-of-16" -> "quarter-finals";
            case "quarter-finals" -> "semi-finals";
            case "semi-finals" -> "final";
            default -> null;
        };
    }

    // Helper methods for updating statistics
    private void updateTeamStats(List<Group.TeamScore> teams, String team1Id, String team2Id, Integer team1Score, Integer team2Score) {
        for (Group.TeamScore team : teams) {
            if (team.getTeamId().equals(team1Id)) {
                team.setPlayed(team.getPlayed() + 1);
                team.setGoalsFor(team.getGoalsFor() + team1Score);
                team.setGoalsAgainst(team.getGoalsAgainst() + team2Score);
                team.setGoalDifference(team.getGoalsFor() - team.getGoalsAgainst());
                
                if (team1Score > team2Score) {
                    team.setWon(team.getWon() + 1);
                    team.setPoints(team.getPoints() + 3);
                } else if (team1Score < team2Score) {
                    team.setLost(team.getLost() + 1);
                } else {
                    team.setDrawn(team.getDrawn() + 1);
                    team.setPoints(team.getPoints() + 1);
                }
            } else if (team.getTeamId().equals(team2Id)) {
                team.setPlayed(team.getPlayed() + 1);
                team.setGoalsFor(team.getGoalsFor() + team2Score);
                team.setGoalsAgainst(team.getGoalsAgainst() + team1Score);
                team.setGoalDifference(team.getGoalsFor() - team.getGoalsAgainst());
                
                if (team2Score > team1Score) {
                    team.setWon(team.getWon() + 1);
                    team.setPoints(team.getPoints() + 3);
                } else if (team2Score < team1Score) {
                    team.setLost(team.getLost() + 1);
                } else {
                    team.setDrawn(team.getDrawn() + 1);
                    team.setPoints(team.getPoints() + 1);
                }
            }
        }
    }

    private void recalculateGroupPositions(List<Group.TeamScore> teams) {
        // Sort by points (desc), goal difference (desc), goals for (desc)
        teams.sort((t1, t2) -> {
            int pointsCompare = Integer.compare(t2.getPoints(), t1.getPoints());
            if (pointsCompare != 0) return pointsCompare;
            
            int goalDiffCompare = Integer.compare(t2.getGoalDifference(), t1.getGoalDifference());
            if (goalDiffCompare != 0) return goalDiffCompare;
            
            return Integer.compare(t2.getGoalsFor(), t1.getGoalsFor());
        });
        
        // Set positions
        for (int i = 0; i < teams.size(); i++) {
            teams.get(i).setPosition(i + 1);
        }
    }

    private void updateQualifiedTeams(Group.TournamentStructure.GroupStage groupStage) {
        if (groupStage.getTeamsQualify() == null) {
            groupStage.setTeamsQualify(2); // Default: top 2
        }
        
        List<String> qualified = new ArrayList<>();
        List<Group.TeamScore> sortedTeams = new ArrayList<>(groupStage.getTeams());
        recalculateGroupPositions(sortedTeams);
        
        for (int i = 0; i < Math.min(groupStage.getTeamsQualify(), sortedTeams.size()); i++) {
            qualified.add(sortedTeams.get(i).getTeamId());
        }
        
        groupStage.setQualifiedTeamIds(qualified);
    }

    private void updateNextMatchWithWinner(Group group, String nextMatchId, String nextStageId, String winnerTeamId) {
        // Find next match and set winner as team1 or team2
        // This is a simplified version - you may need more complex logic based on bracket structure
        if (group.getTournamentStructure() != null && group.getTournamentStructure().getStages() != null) {
            Group.TournamentStructure.Stage nextStage = group.getTournamentStructure().getStages().get(nextStageId);
            if (nextStage != null && nextStage.getMatches() != null) {
                for (Group.TournamentStructure.Match nextMatch : nextStage.getMatches()) {
                    if (nextMatch.getMatchId().equals(nextMatchId)) {
                        // Set winner as team1 if team1 is empty, otherwise team2
                        if (nextMatch.getTeam1Id() == null) {
                            nextMatch.setTeam1Id(winnerTeamId);
                            // You may need to fetch team name and flag from the group
                        } else if (nextMatch.getTeam2Id() == null) {
                            nextMatch.setTeam2Id(winnerTeamId);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Update user prediction scores directly in the match
     */
    public ResponseEntity<Map<String, Object>> updateMatchPrediction(String groupId, String matchId, String userId, Map<String, Object> predictionData) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }
            
            Group group = groupOpt.get();
            
            // Verify user has access to this group
            if (!isUserInGroup(group, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have access to this group"
                ));
            }
            
            // Verify match exists
            Group.TournamentStructure.Match match = findMatchInGroup(group, matchId);
            if (match == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Match not found"
                ));
            }
            
            // Check if match is already played (can't predict after match is played)
            if (match.getIsPlayed() != null && match.getIsPlayed()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Cannot predict on a match that has already been played"
                ));
            }
            
            // Get prediction scores from request
            Integer userTeam1Score = predictionData.get("userTeam1Score") != null ? 
                ((Number) predictionData.get("userTeam1Score")).intValue() : null;
            Integer userTeam2Score = predictionData.get("userTeam2Score") != null ? 
                ((Number) predictionData.get("userTeam2Score")).intValue() : null;
            
            // Validate that at least one score is provided
            if (userTeam1Score == null && userTeam2Score == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "At least one of userTeam1Score or userTeam2Score must be provided"
                ));
            }
            
            // Update prediction scores in the match
            if (userTeam1Score != null) {
                match.setUserTeam1Score(userTeam1Score);
            }
            if (userTeam2Score != null) {
                match.setUserTeam2Score(userTeam2Score);
            }
            
            // Save the group
            groupsRepository.save(group);
            
            return ResponseEntity.ok(Map.of(
                "message", "Match prediction updated successfully",
                "match", match
            ));
        } catch (Exception e) {
            System.err.println("❌ Error updating match prediction: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error updating match prediction: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Save or update a match prediction for a user
     */
    public ResponseEntity<Map<String, Object>> savePrediction(String groupId, String matchId, String userId, Map<String, Object> predictionData, String jwtToken) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }
            
            Group group = groupOpt.get();
            
            // Verify user has access to this group
            if (!isUserInGroup(group, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have access to this group"
                ));
            }
            
            // Verify match exists
            Group.TournamentStructure.Match match = findMatchInGroup(group, matchId);
            if (match == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Match not found"
                ));
            }
            
            // Check if match is already played (can't predict after match is played)
            if (match.getIsPlayed() != null && match.getIsPlayed()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Cannot predict on a match that has already been played"
                ));
            }
            
            Integer team1Score = (Integer) predictionData.get("team1Score");
            Integer team2Score = (Integer) predictionData.get("team2Score");
            
            if (team1Score == null || team2Score == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "team1Score and team2Score are required"
                ));
            }
            
            // Save prediction via auth_service
            Map<String, Object> result = authClientService.savePrediction(userId, groupId, matchId, team1Score, team2Score, jwtToken);
            
            if (result.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Prediction saved successfully",
                "prediction", result
            ));
        } catch (Exception e) {
            System.err.println("❌ Error saving prediction: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error saving prediction: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get all predictions for a user in a group
     */
    public ResponseEntity<Map<String, Object>> getUserPredictions(String groupId, String userId, String jwtToken) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }
            
            Group group = groupOpt.get();
            
            // Verify user has access
            if (!isUserInGroup(group, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have access to this group"
                ));
            }
            
            // Get predictions from auth_service
            Map<String, Object> result = authClientService.getUserPredictions(userId, groupId, jwtToken);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ Error getting predictions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error getting predictions: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get a specific prediction for a user, group, and match
     */
    public ResponseEntity<Map<String, Object>> getUserPrediction(String groupId, String matchId, String userId, String jwtToken) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }
            
            Group group = groupOpt.get();
            
            // Verify user has access
            if (!isUserInGroup(group, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You don't have access to this group"
                ));
            }
            
            // Get prediction from auth_service
            Map<String, Object> result = authClientService.getUserPrediction(userId, groupId, matchId, jwtToken);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ Error getting prediction: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error getting prediction: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Calculate scores for all users' predictions in a group
     * This compares predictions vs actual results and assigns points
     * Points system:
     * - Exact score: 3 points
     * - Correct result (win/draw/loss): 1 point
     * - Wrong result: 0 points
     */
    public ResponseEntity<Map<String, Object>> calculateScores(String groupId, String userId, String jwtToken) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }
            
            Group group = groupOpt.get();
            
            // Only creator can trigger score calculation (or you can allow all members)
            if (!group.getCreatorUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Only the group creator can calculate scores"
                ));
            }
            
            // Get all users in the group
            List<String> allUserIds = new ArrayList<>(getUserIdsFromUsers(group.getUsers()));
            if (!allUserIds.contains(group.getCreatorUserId())) {
                allUserIds.add(group.getCreatorUserId());
            }
            if (!allUserIds.contains(group.getCreatorUserId())) {
                allUserIds.add(group.getCreatorUserId());
            }
            
            Map<String, Integer> userTotalScores = new HashMap<>();
            int totalMatchesProcessed = 0;
            
            // Iterate through all matches in the tournament structure
            if (group.getTournamentStructure() != null && group.getTournamentStructure().getStages() != null) {
                for (Group.TournamentStructure.Stage stage : group.getTournamentStructure().getStages().values()) {
                    // Process group stage matches
                    if (stage.getGroups() != null) {
                        for (Group.TournamentStructure.GroupStage groupStage : stage.getGroups()) {
                            if (groupStage.getMatches() != null) {
                                for (Group.TournamentStructure.Match match : groupStage.getMatches()) {
                                    if (match.getIsPlayed() != null && match.getIsPlayed()) {
                                        // Calculate scores for this match
                                        calculateMatchScores(groupId, match.getMatchId(), match.getTeam1Score(), 
                                            match.getTeam2Score(), allUserIds, jwtToken, userTotalScores);
                                        totalMatchesProcessed++;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Process knockout matches
                    if (stage.getMatches() != null) {
                        for (Group.TournamentStructure.Match match : stage.getMatches()) {
                            if (match.getIsPlayed() != null && match.getIsPlayed()) {
                                // Calculate scores for this match
                                calculateMatchScores(groupId, match.getMatchId(), match.getTeam1Score(), 
                                    match.getTeam2Score(), allUserIds, jwtToken, userTotalScores);
                                totalMatchesProcessed++;
                            }
                        }
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Scores calculated successfully",
                "groupId", groupId,
                "totalMatchesProcessed", totalMatchesProcessed,
                "userScores", userTotalScores
            ));
        } catch (Exception e) {
            System.err.println("❌ Error calculating scores: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error calculating scores: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Helper method to calculate scores for a specific match
     */
    private void calculateMatchScores(String groupId, String matchId, Integer actualTeam1Score, 
            Integer actualTeam2Score, List<String> userIds, String jwtToken, Map<String, Integer> userTotalScores) {
        
        if (actualTeam1Score == null || actualTeam2Score == null) {
            return; // Match not played yet
        }
        
        // Determine actual result
        String actualResult;
        if (actualTeam1Score > actualTeam2Score) {
            actualResult = "team1_win";
        } else if (actualTeam2Score > actualTeam1Score) {
            actualResult = "team2_win";
        } else {
            actualResult = "draw";
        }
        
        // Calculate scores for each user
        for (String userId : userIds) {
            Map<String, Object> predictionResult = authClientService.getUserPrediction(userId, groupId, matchId, jwtToken);
            
            if (predictionResult.containsKey("exists") && (Boolean) predictionResult.get("exists")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> prediction = (Map<String, Object>) predictionResult.get("prediction");
                
                if (prediction != null) {
                    Integer predTeam1Score = (Integer) prediction.get("team1Score");
                    Integer predTeam2Score = (Integer) prediction.get("team2Score");
                    
                    if (predTeam1Score != null && predTeam2Score != null) {
                        int points = 0;
                        
                        // Check for exact score match
                        if (predTeam1Score.equals(actualTeam1Score) && predTeam2Score.equals(actualTeam2Score)) {
                            points = 3; // Exact score: 3 points
                        } else {
                            // Check for correct result
                            String predictedResult;
                            if (predTeam1Score > predTeam2Score) {
                                predictedResult = "team1_win";
                            } else if (predTeam2Score > predTeam1Score) {
                                predictedResult = "team2_win";
                            } else {
                                predictedResult = "draw";
                            }
                            
                            if (predictedResult.equals(actualResult)) {
                                points = 1; // Correct result: 1 point
                            } else {
                                points = 0; // Wrong result: 0 points
                            }
                        }
                        
                        // Update prediction points in auth_service
                        authClientService.updatePredictionPoints(userId, groupId, matchId, points, jwtToken);
                        
                        // Add to total score
                        userTotalScores.put(userId, userTotalScores.getOrDefault(userId, 0) + points);
                    }
                }
            }
        }
    }
    
    /**
     * Helper method to find a match in a group
     */
    private Group.TournamentStructure.Match findMatchInGroup(Group group, String matchId) {
        if (group.getTournamentStructure() != null && group.getTournamentStructure().getStages() != null) {
            for (Group.TournamentStructure.Stage stage : group.getTournamentStructure().getStages().values()) {
                // Search in group stage matches
                if (stage.getGroups() != null) {
                    for (Group.TournamentStructure.GroupStage groupStage : stage.getGroups()) {
                        if (groupStage.getMatches() != null) {
                            for (Group.TournamentStructure.Match match : groupStage.getMatches()) {
                                if (match.getMatchId().equals(matchId)) {
                                    return match;
                                }
                            }
                        }
                    }
                }
                
                // Search in knockout matches
                if (stage.getMatches() != null) {
                    for (Group.TournamentStructure.Match match : stage.getMatches()) {
                        if (match.getMatchId().equals(matchId)) {
                            return match;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Recalculate equitable amount per user based on total users
     * Updates payment amounts for all users
     */
    private void recalculateEquitableAmount(Group group) {
        if (group.getTotalBetAmount() == null || group.getTotalBetAmount() <= 0) {
            return;
        }
        
        // Count total users (including invited emails who haven't joined yet)
        int totalUsers = (group.getUsers() != null ? group.getUsers().size() : 0);
        if (group.getInvitedEmails() != null) {
            totalUsers += group.getInvitedEmails().size();
        }
        
        // If no users, set to totalBetAmount (creator only)
        double equitableAmount;
        if (totalUsers == 0) {
            equitableAmount = group.getTotalBetAmount();
        } else {
            // Divide total amount equally among all users
            equitableAmount = group.getTotalBetAmount() / totalUsers;
        }
        
        group.setEquitableAmountPerUser(equitableAmount);
        
        // Update payment amounts for all existing users
        if (group.getUserPayments() != null) {
            for (Group.UserPayment userPayment : group.getUserPayments().values()) {
                // Only update if user hasn't paid yet (preserve paid amounts)
                if (!userPayment.getHasPaid()) {
                    userPayment.setPaymentAmount(equitableAmount);
                }
            }
        }
    }
    
    /**
     * Update user payment status after successful payment
     * Called by payments service after payment confirmation
     */
    public ResponseEntity<Map<String, Object>> updateUserPayment(
            String groupId, String userId, String paymentId) {
        try {
            Optional<Group> groupOpt = groupsRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Group not found"
                ));
            }

            Group group = groupOpt.get();
            
            if (group.getUserPayments() == null || !group.getUserPayments().containsKey(userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "User payment record not found"
                ));
            }
            
            Group.UserPayment userPayment = group.getUserPayments().get(userId);
            userPayment.setHasPaid(true);
            userPayment.setPaymentId(paymentId);
            userPayment.setPaidDate(new Date());
            
            group.setUpdatedAt(new Date());
            Group updatedGroup = groupsRepository.save(group);
            
            return ResponseEntity.ok(Map.of(
                "message", "User payment status updated successfully",
                "group", updatedGroup
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error updating user payment: " + e.getMessage()
            ));
        }
    }
    
    // ========== HELPER METHODS FOR USERS ==========
    
    /**
     * Check if a user is in the group (either as creator or member)
     */
    private boolean isUserInGroup(Group group, String userId) {
        if (group.getCreatorUserId().equals(userId)) {
            return true;
        }
        if (group.getUsers() != null) {
            for (Group.GroupUser user : group.getUsers()) {
                if (user.getId().equals(userId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Get list of user IDs from users list
     */
    private List<String> getUserIdsFromUsers(List<Group.GroupUser> users) {
        List<String> userIds = new ArrayList<>();
        if (users != null) {
            for (Group.GroupUser user : users) {
                userIds.add(user.getId());
            }
        }
        return userIds;
    }
    
    /**
     * Add a user to the group's users list
     */
    private void addUserToGroup(Group group, String userId, String userName, String jwtToken) {
        if (group.getUsers() == null) {
            group.setUsers(new ArrayList<>());
        }
        
        // Check if user already exists
        for (Group.GroupUser user : group.getUsers()) {
            if (user.getId().equals(userId)) {
                return; // User already exists
            }
        }
        
        // Get user name from auth_service if not provided
        if (userName == null || userName.isEmpty()) {
            Map<String, Object> userInfo = authClientService.getUserById(userId, jwtToken);
            userName = userInfo.get("name") != null ? userInfo.get("name").toString() : 
                      (userInfo.get("firstName") != null ? userInfo.get("firstName").toString() : 
                      (userInfo.get("email") != null ? ((String) userInfo.get("email")).split("@")[0] : "Usuario"));
        }
        
        Group.GroupUser newUser = new Group.GroupUser();
        newUser.setId(userId);
        newUser.setNombre(userName);
        newUser.setScore(0);
        group.getUsers().add(newUser);
    }

    /**
     * Normalize GroupUser objects: convert _id to id if present
     * This handles MongoDB documents that were saved with _id instead of id
     */
    private void normalizeGroupUsers(Group group) {
        if (group == null || group.getUsers() == null) {
            return;
        }
        
        for (Group.GroupUser user : group.getUsers()) {
            // If id is null but we have _id in the document, we need to handle it
            // This is a workaround for MongoDB documents that have _id instead of id
            // The @JsonAlias should handle this during JSON deserialization,
            // but we also need to ensure the object is properly initialized
            if (user.getId() == null || user.getId().isEmpty()) {
                // This shouldn't happen if MongoDB mapping is correct
                // But we handle it just in case
                System.err.println("⚠️ WARNING: GroupUser has null or empty id");
            }
        }
    }

    /**
     * Normalize Match objects: convert matchday from Date to Integer if needed
     * Some MongoDB documents have matchday as Date instead of Integer
     */
    private void normalizeMatches(Group group) {
        if (group == null || group.getTournamentStructure() == null || 
            group.getTournamentStructure().getStages() == null) {
            return;
        }
        
        for (Group.TournamentStructure.Stage stage : group.getTournamentStructure().getStages().values()) {
            if (stage == null) {
                continue;
            }
            
            // Normalize matches in group stage
            if (stage.getGroups() != null) {
                for (Group.TournamentStructure.GroupStage groupStage : stage.getGroups()) {
                    if (groupStage != null && groupStage.getMatches() != null) {
                        for (Group.TournamentStructure.Match match : groupStage.getMatches()) {
                            normalizeMatch(match);
                        }
                    }
                }
            }
            
            // Normalize matches in knockout stages
            if (stage.getMatches() != null) {
                for (Group.TournamentStructure.Match match : stage.getMatches()) {
                    normalizeMatch(match);
                }
            }
        }
    }

    /**
     * Normalize a single match: convert matchday from Date to Integer if needed
     * The getMatchday() method already handles this, but we call it here to ensure
     * the value is properly converted and stored
     */
    private void normalizeMatch(Group.TournamentStructure.Match match) {
        if (match == null) {
            return;
        }
        
        // Get the matchday value (this will convert Date to Integer if needed)
        Integer matchday = match.getMatchday();
        // Set it back to ensure it's stored as Integer
        if (matchday != null) {
            match.setMatchday(matchday);
        }
    }

    /**
     * Normalize a group after reading from MongoDB
     * Ensures all fields are properly mapped
     */
    private Group normalizeGroup(Group group) {
        if (group == null) {
            return null;
        }
        
        normalizeGroupUsers(group);
        normalizeMatches(group);
        return group;
    }
}

