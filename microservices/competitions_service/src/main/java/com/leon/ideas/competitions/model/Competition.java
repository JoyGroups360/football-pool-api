package com.leon.ideas.competitions.model;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class Competition {
    private String id;
    private String name;
    private String shortName;
    private String region;
    private String type;
    private String frequency;
    private String image;
    private String country;
    private Date poolaAvailableDay;
    private Date poolDisbaledDate;
    private List<QualifiedTeam> qualifiedTeams;
    
    // Tournament Structure - Global structure with REAL match results
    // This is shared across all groups using this competition
    private TournamentStructure tournamentStructure;
    
    // Groups Kind Tournament - Specific structure for group-based tournaments
    // This field will be used for tournaments that start with groups (like FIFA Club World Cup)
    // Later, other tournament types can be added
    private TournamentStructure groupsKindTournament;
    
    @Data
    public static class QualifiedTeam {
        private String id;
        private String name;
        private String country;
        private String flag; // For national teams (country flags)
        private String image; // For club teams (club logos/badges)
        private String group;
        private Integer seed;
    }
}


