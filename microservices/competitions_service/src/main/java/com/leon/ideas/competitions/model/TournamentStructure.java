package com.leon.ideas.competitions.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Tournament Structure - Global structure for all groups using this competition
 * Contains all matches with REAL results (team1Score, team2Score, etc.)
 * This is shared across all groups using the same competition
 */
@Data
public class TournamentStructure {
    private String tournamentFormat; // "groups-then-knockout", "only-groups", "only-knockout", "custom"
    private String currentStage; // Current active stage ID
    private Map<String, Stage> stages; // Key: stageId, Value: Stage data
    private TournamentConfig config; // Tournament configuration
    
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
        private String stageId; // "group-stage", "round-of-16", etc.
        private String stageName;
        private String type; // "groups" or "knockout"
        private Boolean isActive;
        private Boolean isCompleted;
        private Integer order;
        
        // For group-stage type
        private List<GroupStage> groups;
        
        // For knockout type
        private List<Match> matches;
        
        private List<String> qualifiedTeamIds;
    }
    
    @Data
    public static class GroupStage {
        private String groupLetter; // "A", "B", "C", etc.
        private String groupName;
        private List<TeamScore> teams;
        private List<String> qualifiedTeamIds;
        private Integer teamsPerGroup;
        private Integer teamsQualify;
        private List<Match> matches; // All matches in this group with REAL results
    }
    
    /**
     * Match - Contains REAL match results (global, shared by all groups)
     * This is the source of truth for match results
     */
    @Data
    public static class Match {
        private String matchId; // Unique match ID (global)
        private String matchNumber;
        private String stageId;
        private String groupLetter; // If group stage, which group
        
        // Teams
        private String team1Id;
        private String team1Name;
        private String team1Flag;
        private String team2Id;
        private String team2Name;
        private String team2Flag;
        
        // REAL Match results (only backend/admin can modify)
        private Integer team1Score; // Real score for team1
        private Integer team2Score; // Real score for team2
        private String winnerTeamId;
        private String loserTeamId;
        private Boolean isDraw;
        
        // Extra time and penalties (only for knockout stages)
        private Boolean extraTime; // Did the match go to extra time?
        private Boolean penalties; // Did the match go to penalties?
        private Integer penaltiesTeam1Score; // Real penalties score for team1
        private Integer penaltiesTeam2Score; // Real penalties score for team2
        
        // Match metadata
        private Date matchDate; // When the match is scheduled
        private Date playedDate; // When the match was actually played
        private Boolean isPlayed; // Has the match been played?
        private String venue;
        private String status; // "scheduled", "in-progress", "finished", "postponed", "cancelled"
        
        // For knockout matches
        private String nextMatchId;
        private String nextStageId;
        
        // For group stage matches
        @Field("matchday")
        @JsonIgnore
        private Object matchdayInternal;
        
        @JsonProperty("matchday")
        public Integer getMatchday() {
            if (matchdayInternal == null) {
                return null;
            }
            if (matchdayInternal instanceof Integer) {
                return (Integer) matchdayInternal;
            }
            if (matchdayInternal instanceof Number) {
                return ((Number) matchdayInternal).intValue();
            }
            return null;
        }
        
        public void setMatchday(Integer matchday) {
            this.matchdayInternal = matchday;
        }
    }
    
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
        private Integer position;
    }
}


import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Tournament Structure - Global structure for all groups using this competition
 * Contains all matches with REAL results (team1Score, team2Score, etc.)
 * This is shared across all groups using the same competition
 */
@Data
public class TournamentStructure {
    private String tournamentFormat; // "groups-then-knockout", "only-groups", "only-knockout", "custom"
    private String currentStage; // Current active stage ID
    private Map<String, Stage> stages; // Key: stageId, Value: Stage data
    private TournamentConfig config; // Tournament configuration
    
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
        private String stageId; // "group-stage", "round-of-16", etc.
        private String stageName;
        private String type; // "groups" or "knockout"
        private Boolean isActive;
        private Boolean isCompleted;
        private Integer order;
        
        // For group-stage type
        private List<GroupStage> groups;
        
        // For knockout type
        private List<Match> matches;
        
        private List<String> qualifiedTeamIds;
    }
    
    @Data
    public static class GroupStage {
        private String groupLetter; // "A", "B", "C", etc.
        private String groupName;
        private List<TeamScore> teams;
        private List<String> qualifiedTeamIds;
        private Integer teamsPerGroup;
        private Integer teamsQualify;
        private List<Match> matches; // All matches in this group with REAL results
    }
    
    /**
     * Match - Contains REAL match results (global, shared by all groups)
     * This is the source of truth for match results
     */
    @Data
    public static class Match {
        private String matchId; // Unique match ID (global)
        private String matchNumber;
        private String stageId;
        private String groupLetter; // If group stage, which group
        
        // Teams
        private String team1Id;
        private String team1Name;
        private String team1Flag;
        private String team2Id;
        private String team2Name;
        private String team2Flag;
        
        // REAL Match results (only backend/admin can modify)
        private Integer team1Score; // Real score for team1
        private Integer team2Score; // Real score for team2
        private String winnerTeamId;
        private String loserTeamId;
        private Boolean isDraw;
        
        // Extra time and penalties (only for knockout stages)
        private Boolean extraTime; // Did the match go to extra time?
        private Boolean penalties; // Did the match go to penalties?
        private Integer penaltiesTeam1Score; // Real penalties score for team1
        private Integer penaltiesTeam2Score; // Real penalties score for team2
        
        // Match metadata
        private Date matchDate; // When the match is scheduled
        private Date playedDate; // When the match was actually played
        private Boolean isPlayed; // Has the match been played?
        private String venue;
        private String status; // "scheduled", "in-progress", "finished", "postponed", "cancelled"
        
        // For knockout matches
        private String nextMatchId;
        private String nextStageId;
        
        // For group stage matches
        @Field("matchday")
        @JsonIgnore
        private Object matchdayInternal;
        
        @JsonProperty("matchday")
        public Integer getMatchday() {
            if (matchdayInternal == null) {
                return null;
            }
            if (matchdayInternal instanceof Integer) {
                return (Integer) matchdayInternal;
            }
            if (matchdayInternal instanceof Number) {
                return ((Number) matchdayInternal).intValue();
            }
            return null;
        }
        
        public void setMatchday(Integer matchday) {
            this.matchdayInternal = matchday;
        }
    }
    
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
        private Integer position;
    }
}


