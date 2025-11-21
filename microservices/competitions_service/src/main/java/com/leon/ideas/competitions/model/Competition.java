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
    
    @Data
    public static class QualifiedTeam {
        private String id;
        private String name;
        private String country;
        private String flag;
        private String group;
        private Integer seed;
    }
}


