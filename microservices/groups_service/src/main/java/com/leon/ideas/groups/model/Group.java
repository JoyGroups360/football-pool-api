package com.leon.ideas.groups.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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
    
    // Tournament structure (DEPRECATED - results should come from competitions_service)
    // This is kept for backward compatibility, but real match results (team1Score, team2Score)
    // should be stored in competitions_service, not here
    private TournamentStructure tournamentStructure;
    
    // User predictions for matches in this group
    // List of predictions where each prediction has userId and matchId
    // Real match results are stored in competitions_service
    private List<UserPrediction> userPredictions;
    
    // Matches detail - Array of match info (same as matchInfo from predictions)
    // This contains all the match predictions with real scores for the competition
    private List<Map<String, Object>> matchesDetail;
    
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
        @Field("_id") // MongoDB stores as _id, map it to id field
        @com.fasterxml.jackson.annotation.JsonProperty("id") // Always serialize as "id" in JSON (not _id)
        private String id; // User ID (same as userId, kept for backward compatibility)
        private String userId; // User ID (explicit field)
        private String nombre; // User name
        private Integer score; // User score in the group
        private List<Map<String, Object>> matchesInfo; // Match info from user's predictions for this competition
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
     * Scoreboard - stores user scores and rankings
     */
    @Data
    public static class Scoreboard {
        private List<UserScore> users; // List of users with their scores
        
        @Data
        public static class UserScore {
            private String userId; // User ID
            private String userName; // User name (for display)
            private Integer score; // Total points accumulated
            private Integer position; // Ranking position (1 = first place, 2 = second, etc.)
            private Date lastUpdated; // Last time the score was updated
        }
    }
    
    /**
     * Tournament Structure - handles all stages of the tournament
     * DEPRECATED: Real match results (team1Score, team2Score) should be in competitions_service
     * This is kept for backward compatibility and structure reference
     */
    @Data
    public static class TournamentStructure {
        private String tournamentFormat;
        private String currentStage;
        private Map<String, Stage> stages;
        private TournamentConfig config;
        
        @Data
        public static class TournamentConfig {
            private Integer totalTeams;
            private Integer numberOfGroups;
            private Integer teamsPerGroup;
            private Integer teamsQualifyPerGroup;
            private Boolean hasGroupStage;
            private Boolean hasKnockoutStage;
            private List<String> knockoutRounds;
        }
        
        @Data
        public static class Stage {
            private String stageId;
            private String stageName;
            private String type;
            private Boolean isActive;
            private Boolean isCompleted;
            private Integer order;
            private List<GroupStage> groups;
            private List<Match> matches;
            private List<String> qualifiedTeamIds;
        }
        
        @Data
        public static class GroupStage {
            private String groupLetter;
            private String groupName;
            private List<TeamScore> teams;
            private List<String> qualifiedTeamIds;
            private Integer teamsPerGroup;
            private Integer teamsQualify;
            private List<Match> matches;
        }
        
        /**
         * Match - DEPRECATED: Real results should be in competitions_service
         * This structure is kept for backward compatibility
         */
        @Data
        public static class Match {
            private String matchId;
            private String matchNumber;
            private String stageId;
            private String groupLetter;
            private String team1Id;
            private String team1Name;
            private String team1Flag;
            private String team2Id;
            private String team2Name;
            private String team2Flag;
            
            // DEPRECATED: Real results should be in competitions_service
            private Integer team1Score;
            private Integer team2Score;
            private String winnerTeamId;
            private String loserTeamId;
            private Boolean isDraw;
            
            // User predictions (these can stay in groups)
            private Integer userTeam1Score;
            private Integer userTeam2Score;
            private Boolean userExtraTime;
            private Boolean userPenalties;
            private Integer userPenaltiesTeam1Score;
            private Integer userPenaltiesTeam2Score;
            
            // Extra time and penalties (real results - should be in competitions)
            private Boolean extraTime;
            private Boolean penalties;
            private Integer penaltiesTeam1Score;
            private Integer penaltiesTeam2Score;
            
            private Date matchDate;
            private Date playedDate;
            private Boolean isPlayed;
            private String venue;
            private String status;
            private String nextMatchId;
            private String nextStageId;
            
            @Field("matchday")
            @com.fasterxml.jackson.annotation.JsonIgnore
            private Object matchdayInternal;
            
            @com.fasterxml.jackson.annotation.JsonProperty("matchday")
            public Integer getMatchday() {
                if (matchdayInternal == null) return null;
                if (matchdayInternal instanceof Integer) return (Integer) matchdayInternal;
                if (matchdayInternal instanceof Number) return ((Number) matchdayInternal).intValue();
                return null;
            }
            
            public void setMatchday(Integer matchday) {
                this.matchdayInternal = matchday;
            }
        }
    }
    
    /**
     * User Prediction - Contains ONLY user predictions for a match
     * Real match results are stored in competitions_service
     */
    @Data
    public static class UserPrediction {
        private String matchId; // Reference to match in competitions
        private String userId; // User who made this prediction
        
        // User prediction scores (frontend can modify these)
        private Integer userTeam1Score; // User's predicted score for team1
        private Integer userTeam2Score; // User's predicted score for team2
        
        // User predictions for extra time and penalties (only for knockout stages)
        private Boolean userExtraTime; // User predicted extra time (only for knockout)
        private Boolean userPenalties; // User predicted penalties (only for knockout)
        private Integer userPenaltiesTeam1Score; // User's predicted penalties score for team1 (only for knockout)
        private Integer userPenaltiesTeam2Score; // User's predicted penalties score for team2 (only for knockout)
        
        private Date predictedDate; // When the user made this prediction
        private Integer points; // Points earned for this prediction (calculated after match is played)
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


