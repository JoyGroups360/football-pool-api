package com.leon.ideas.groups.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "groups")
public class Group {
    @Id
    private String groupId;
    private String creatorUserId;
    private String competitionId;
    private String competitionName; // Cached from competitions_service
    private String competitionImage; // Cached from competitions_service
    private String name; // Group name
    private List<String> teamIds; // All teams in the competition (deprecated, use stages)
    private List<GroupUser> users; // List of users in the group (replaces userIds)
    private List<String> invitedEmails; // Emails invited but not registered yet
    
    // Legacy scoreboard (for backward compatibility - phase 1: group stage)
    private Scoreboard scoreboard;
    
    // New structure: Tournament stages (groups, round-of-16, quarter-finals, semi-finals, third-place, final)
    private TournamentStructure tournamentStructure;
    
    private Date createdAt;
    private Date updatedAt;
    private Date enabledAt;
    private Date disabledAt;
    
    // Betting information
    private Double totalBetAmount; // Total amount that the group must pay (betting pool)
    private Date paymentDeadline; // Last day to pay (same date when users can no longer be created)
    
    // Individual user payments (each user pays their equitable share)
    private Map<String, UserPayment> userPayments; // Key: userId, Value: UserPayment details
    private Double equitableAmountPerUser; // Calculated: totalBetAmount / numberOfUsers (each user pays this amount)
    
    /**
     * GroupUser - User information in the group
     */
    @Data
    public static class GroupUser {
        private String id; // User ID
        private String nombre; // User name
        private Integer score; // User score in the group
    }
    
    /**
     * User Payment - Payment information for each user (equitable share)
     */
    @Data
    public static class UserPayment {
        private String userId;
        private String userEmail;
        private Double paymentAmount; // Amount this user must pay (equitable share)
        private Boolean hasPaid; // Has the user completed payment?
        private String paymentId; // Reference to payment in payments_service
        private Date paidDate; // When the user completed payment
        private Boolean isCreator; // Is this user the group creator?
    }
    
    /**
     * Legacy Scoreboard - kept for backward compatibility
     * Used for group stage phase
     */
    @Data
    public static class Scoreboard {
        private List<TeamScore> teams;
        
        @Data
        public static class TeamScore {
            private String teamId;
            private String teamName;
            private String teamFlag;
            private Integer played;
            private Integer won;
            private Integer drawn;
            private Integer lost;
            private Integer goalsFor;
            private Integer goalsAgainst;
            private Integer goalDifference;
            private Integer points;
        }
    }
    
    /**
     * Tournament Structure - handles all stages of the tournament
     * Flexible structure that supports different tournament formats
     */
    @Data
    public static class TournamentStructure {
        private String tournamentFormat; // "groups-then-knockout", "only-groups", "only-knockout", "custom"
        private String currentStage; // Current active stage ID
        private Map<String, Stage> stages; // Key: stageId, Value: Stage data
        private TournamentConfig config; // Tournament configuration (number of groups, teams per group, etc.)
        
        /**
         * Tournament Configuration - defines the tournament format
         */
        @Data
        public static class TournamentConfig {
            private Integer totalTeams; // Total number of teams in the tournament
            private Integer numberOfGroups; // Number of groups (if applicable)
            private Integer teamsPerGroup; // Teams per group (if applicable)
            private Integer teamsQualifyPerGroup; // How many teams qualify from each group (usually 2)
            private Boolean hasGroupStage; // Does this tournament have a group stage?
            private Boolean hasKnockoutStage; // Does this tournament have knockout rounds?
            private List<String> knockoutRounds; // List of knockout rounds: ["round-of-16", "quarter-finals", "semi-finals", "third-place", "final"]
        }
        
        @Data
        public static class Stage {
            private String stageId; // "group-stage", "round-of-16", etc.
            private String stageName; // "Fase de Grupos", "Octavos de Final", etc.
            private String type; // "groups" or "knockout"
            private Boolean isActive; // Is this stage currently active?
            private Boolean isCompleted; // Has this stage finished?
            private Integer order; // Order of this stage (1, 2, 3, etc.)
            
            // For group-stage type
            private List<GroupStage> groups; // Only used when type = "groups"
            
            // For knockout type (round-of-16, quarter-finals, etc.)
            private List<Match> matches; // All matches in this stage (both groups and knockout use this)
            
            private List<String> qualifiedTeamIds; // Teams that advanced to next stage
        }
        
        /**
         * Group Stage - for phase 1 (groups)
         * Example: Group A, Group B, etc. with 4 teams each, top 2 qualify
         */
        @Data
        public static class GroupStage {
            private String groupLetter; // "A", "B", "C", etc.
            private String groupName; // "Grupo A"
            private List<TeamScore> teams; // Teams in this group with their stats
            private List<String> qualifiedTeamIds; // Top teams that qualify (sorted by points, goal difference, etc.)
            private Integer teamsPerGroup; // Usually 4
            private Integer teamsQualify; // Usually 2 (top 2)
            private List<Match> matches; // All matches played in this group
        }
        
        /**
         * Match - Universal match structure for both group stage and knockout rounds
         * This allows registering individual match results
         */
        @Data
        public static class Match {
            private String matchId; // Unique match ID
            private String matchNumber; // "1", "2", "3", etc. (within the stage/group)
            private String stageId; // Which stage this match belongs to
            private String groupLetter; // If group stage, which group (null for knockout)
            
            // Teams
            private String team1Id;
            private String team1Name;
            private String team1Flag;
            private String team2Id;
            private String team2Name;
            private String team2Flag;
            
            // Match result
            private Integer team1Score; // null if not played yet
            private Integer team2Score; // null if not played yet
            private String winnerTeamId; // null if not played yet (or if draw)
            private String loserTeamId; // null if not played yet (or if draw)
            private Boolean isDraw; // true if match ended in a draw
            
            // Match metadata
            private Date matchDate; // When the match is scheduled
            private Date playedDate; // When the match was actually played
            private Boolean isPlayed; // Has the match been played?
            private String venue; // Stadium/venue name (optional)
            private String status; // "scheduled", "in-progress", "finished", "postponed", "cancelled"
            
            // For knockout matches
            private String nextMatchId; // Which match does the winner advance to? (null for final)
            private String nextStageId; // Which stage does the winner advance to?
            
            // For group stage matches
            private Integer matchday; // Matchday number (1, 2, 3, etc.) within the group
        }
    }
    
    /**
     * Team Score - used in both legacy Scoreboard and TournamentStructure
     */
    @Data
    public static class TeamScore {
        private String teamId;
        private String teamName;
        private String teamFlag;
        private Integer played;
        private Integer won;
        private Integer drawn;
        private Integer lost;
        private Integer goalsFor;
        private Integer goalsAgainst;
        private Integer goalDifference;
        private Integer points;
        private Integer position; // Position in group/ranking (1st, 2nd, 3rd, 4th)
    }
}


