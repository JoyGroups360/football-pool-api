package com.leon.ideas.competitions.service;

import com.leon.ideas.competitions.repository.CompetitionsRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CompetitionsService {

    @Autowired
    private CompetitionsRepository competitionsRepository;

    /**
     * Get all competitions
     */
    public ResponseEntity<Document> getAllCompetitions() {
        try {
            Document competitions = competitionsRepository.getAllCompetitions();
            if (competitions != null) {
                return new ResponseEntity<>(competitions, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competitions not found"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error retrieving competitions: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get competitions by category
     */
    public ResponseEntity<?> getCompetitionsByCategory(String category) {
        try {
            // Validate category
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category. Valid categories: fifaNationalTeamCups, fifaOfficialClubCups, nationalClubLeagues"),
                    HttpStatus.BAD_REQUEST
                );
            }

            List<Document> competitions = competitionsRepository.getCompetitionsByCategory(category);
            return new ResponseEntity<>(competitions, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error retrieving competitions: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get a specific competition by category and ID
     */
    public ResponseEntity<Document> getCompetitionById(String category, String competitionId) {
        try {
            // Validate category
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            Document competition = competitionsRepository.getCompetitionById(category, competitionId);
            if (competition != null) {
                return new ResponseEntity<>(competition, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competition not found"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error retrieving competition: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Add new competition to a category
     */
    public ResponseEntity<Document> addCompetition(String category, Document competition) {
        try {
            // Validate category
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            // Validate required fields
            if (!competition.containsKey("id") || !competition.containsKey("name")) {
                return new ResponseEntity<>(
                    new Document("error", "Missing required fields: id and name are required"),
                    HttpStatus.BAD_REQUEST
                );
            }

            // Check if competition ID already exists in the category
            Document existing = competitionsRepository.getCompetitionById(category, competition.getString("id"));
            if (existing != null) {
                return new ResponseEntity<>(
                    new Document("error", "Competition with this ID already exists in the category"),
                    HttpStatus.CONFLICT
                );
            }

            boolean success = competitionsRepository.addCompetition(category, competition);
            if (success) {
                return new ResponseEntity<>(
                    new Document("message", "Competition added successfully").append("competition", competition),
                    HttpStatus.CREATED
                );
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Failed to add competition"),
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error adding competition: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Update a competition (PUT - full update)
     */
    public ResponseEntity<Document> updateCompetition(String category, String competitionId, Document updatedCompetition) {
        try {
            // Validate category
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            // Ensure the ID in the body matches the ID in the URL
            updatedCompetition.put("id", competitionId);

            boolean success = competitionsRepository.updateCompetition(category, competitionId, updatedCompetition);
            if (success) {
                return new ResponseEntity<>(
                    new Document("message", "Competition updated successfully").append("competition", updatedCompetition),
                    HttpStatus.OK
                );
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competition not found"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error updating competition: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Patch a competition (PATCH - partial update)
     */
    public ResponseEntity<Document> patchCompetition(String category, String competitionId, Map<String, Object> updates) {
        try {
            // Validate category
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            // Don't allow updating the ID field
            if (updates.containsKey("id")) {
                return new ResponseEntity<>(
                    new Document("error", "Cannot update competition ID"),
                    HttpStatus.BAD_REQUEST
                );
            }

            boolean success = competitionsRepository.patchCompetition(category, competitionId, updates);
            if (success) {
                // Get the updated competition to return
                Document updatedCompetition = competitionsRepository.getCompetitionById(category, competitionId);
                return new ResponseEntity<>(
                    new Document("message", "Competition patched successfully").append("competition", updatedCompetition),
                    HttpStatus.OK
                );
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competition not found"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error patching competition: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Delete a competition from a category
     */
    public ResponseEntity<Document> deleteCompetition(String category, String competitionId) {
        try {
            // Validate category
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            boolean success = competitionsRepository.deleteCompetition(category, competitionId);
            if (success) {
                return new ResponseEntity<>(
                    new Document("message", "Competition deleted successfully"),
                    HttpStatus.OK
                );
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competition not found"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error deleting competition: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Search competitions across all categories
     */
    public ResponseEntity<?> searchCompetitions(String searchTerm) {
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return new ResponseEntity<>(
                    new Document("error", "Search term is required"),
                    HttpStatus.BAD_REQUEST
                );
            }

            List<Document> results = competitionsRepository.searchCompetitions(searchTerm);
            return new ResponseEntity<>(
                new Document("results", results).append("count", results.size()),
                HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error searching competitions: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Add qualified team to a competition
     */
    public ResponseEntity<Document> addQualifiedTeam(String category, String competitionId, Document team) {
        try {
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            // Validate required fields
            if (!team.containsKey("id") || !team.containsKey("name")) {
                return new ResponseEntity<>(
                    new Document("error", "Missing required fields: id and name are required"),
                    HttpStatus.BAD_REQUEST
                );
            }

            boolean success = competitionsRepository.addQualifiedTeam(category, competitionId, team);
            if (success) {
                return new ResponseEntity<>(
                    new Document("message", "Team added successfully").append("team", team),
                    HttpStatus.CREATED
                );
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competition not found or team already exists"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error adding team: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Update a qualified team in a competition
     */
    public ResponseEntity<Document> updateQualifiedTeam(String category, String competitionId, String teamId, Document updatedTeam) {
        try {
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            updatedTeam.put("id", teamId);
            boolean success = competitionsRepository.updateQualifiedTeam(category, competitionId, teamId, updatedTeam);
            if (success) {
                return new ResponseEntity<>(
                    new Document("message", "Team updated successfully").append("team", updatedTeam),
                    HttpStatus.OK
                );
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competition or team not found"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error updating team: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Delete a qualified team from a competition
     */
    public ResponseEntity<Document> deleteQualifiedTeam(String category, String competitionId, String teamId) {
        try {
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            boolean success = competitionsRepository.deleteQualifiedTeam(category, competitionId, teamId);
            if (success) {
                return new ResponseEntity<>(
                    new Document("message", "Team deleted successfully"),
                    HttpStatus.OK
                );
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competition or team not found"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error deleting team: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get all qualified teams for a competition
     */
    public ResponseEntity<?> getQualifiedTeams(String category, String competitionId) {
        try {
            if (!isValidCategory(category)) {
                return new ResponseEntity<>(
                    new Document("error", "Invalid category"),
                    HttpStatus.BAD_REQUEST
                );
            }

            List<Document> teams = competitionsRepository.getQualifiedTeams(category, competitionId);
            if (teams != null) {
                return new ResponseEntity<>(teams, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                    new Document("error", "Competition not found"),
                    HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Error retrieving teams: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Validate if category is valid
     */
    private boolean isValidCategory(String category) {
        return "fifaNationalTeamCups".equals(category) ||
               "fifaOfficialClubCups".equals(category) ||
               "nationalClubLeagues".equals(category);
    }
}


