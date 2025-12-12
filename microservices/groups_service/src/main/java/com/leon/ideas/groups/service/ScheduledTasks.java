package com.leon.ideas.groups.service;

import com.leon.ideas.groups.model.Group;
import com.leon.ideas.groups.repository.GroupsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
public class ScheduledTasks {

    @Autowired
    private GroupsRepository groupsRepository;

    @Autowired
    private GroupsService groupsService;

    /**
     * Recalculate scores for all groups every 2 hours
     * This ensures scores are always up to date even if users don't trigger manual calculation
     */
    @Scheduled(fixedRate = 7200000) // 2 hours in milliseconds (2 * 60 * 60 * 1000)
    public void recalculateAllGroupScores() {
        try {
            System.out.println("🔄 Scheduled task: Recalculating scores for all groups...");
            
            // Get all groups
            List<Group> allGroups = groupsRepository.findAll();
            
            if (allGroups == null || allGroups.isEmpty()) {
                System.out.println("ℹ️ No groups found to recalculate scores");
                return;
            }
            
            int groupsProcessed = 0;
            int groupsWithScores = 0;
            
            for (Group group : allGroups) {
                try {
                    // Get all user IDs in the group
                    List<String> userIds = new java.util.ArrayList<>();
                    if (group.getUsers() != null) {
                        for (Group.GroupUser user : group.getUsers()) {
                            userIds.add(user.getId());
                        }
                    }
                    if (group.getCreatorUserId() != null && !userIds.contains(group.getCreatorUserId())) {
                        userIds.add(group.getCreatorUserId());
                    }
                    
                    if (userIds.isEmpty()) {
                        continue; // Skip groups with no users
                    }
                    
                    // Calculate scores for all users in this group
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
                                            if (match.getIsPlayed() != null && match.getIsPlayed() &&
                                                match.getTeam1Score() != null && match.getTeam2Score() != null) {
                                                // Calculate scores for this match
                                                groupsService.calculateMatchScoresForAllUsers(group, match, userIds, userTotalScores);
                                                totalMatchesProcessed++;
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Process knockout matches
                            if (stage.getMatches() != null) {
                                for (Group.TournamentStructure.Match match : stage.getMatches()) {
                                    if (match.getIsPlayed() != null && match.getIsPlayed() &&
                                        match.getTeam1Score() != null && match.getTeam2Score() != null) {
                                        // Calculate scores for this match
                                        groupsService.calculateMatchScoresForAllUsers(group, match, userIds, userTotalScores);
                                        totalMatchesProcessed++;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Update user scores in the group's users array
                    if (group.getUsers() != null) {
                        for (Group.GroupUser groupUser : group.getUsers()) {
                            String userInGroupId = groupUser.getId();
                            Integer totalScore = userTotalScores.getOrDefault(userInGroupId, 0);
                            groupUser.setScore(totalScore);
                        }
                    }
                    
                    // Update scoreboard
                    if (totalMatchesProcessed > 0) {
                        groupsService.updateScoreboardForGroup(group, userTotalScores);
                        groupsRepository.save(group);
                        groupsWithScores++;
                    }
                    
                    groupsProcessed++;
                } catch (Exception e) {
                    System.err.println("❌ Error recalculating scores for group " + group.getGroupId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("✅ Scheduled task completed: Processed " + groupsProcessed + " groups, " + 
                             groupsWithScores + " groups had scores updated");
        } catch (Exception e) {
            System.err.println("❌ Error in scheduled task for recalculating scores: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
