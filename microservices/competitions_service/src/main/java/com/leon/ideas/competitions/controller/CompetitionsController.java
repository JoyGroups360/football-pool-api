package com.leon.ideas.competitions.controller;

import com.leon.ideas.competitions.service.CompetitionsService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/football-pool/v1/api/competitions")
public class CompetitionsController {

    @Autowired
    private CompetitionsService competitionsService;

    /**
     * GET /football-pool/v1/api/competitions
     * Get all competitions (entire document with all categories)
     */
    @GetMapping
    public ResponseEntity<Document> getAllCompetitions() {
        System.out.println("📋 GET request received: Get all competitions");
        return competitionsService.getAllCompetitions();
    }

    /**
     * GET /football-pool/v1/api/competitions/{category}
     * Get competitions by category
     * 
     * Valid categories:
     * - fifaNationalTeamCups
     * - fifaOfficialClubCups
     * - nationalClubLeagues
     */
    @GetMapping("/{category}")
    public ResponseEntity<?> getCompetitionsByCategory(@PathVariable String category) {
        System.out.println("📋 GET request received: Get competitions by category - " + category);
        return competitionsService.getCompetitionsByCategory(category);
    }

    /**
     * GET /football-pool/v1/api/competitions/{category}/{competitionId}
     * Get a specific competition by category and ID
     */
    @GetMapping("/{category}/{competitionId}")
    public ResponseEntity<Document> getCompetitionById(
            @PathVariable String category,
            @PathVariable String competitionId) {
        System.out.println("📋 GET request received: Get competition - Category: " + category + ", ID: " + competitionId);
        return competitionsService.getCompetitionById(category, competitionId);
    }

    /**
     * POST /football-pool/v1/api/competitions/{category}
     * Add new competition to a category
     * 
     * Required fields in body:
     * - id (String)
     * - name (String)
     * 
     * Optional fields:
     * - shortName, region, type, frequency, image, country
     */
    @PostMapping("/{category}")
    public ResponseEntity<Document> addCompetition(
            @PathVariable String category,
            @RequestBody Document competition) {
        System.out.println("➕ POST request received: Add competition to " + category);
        System.out.println("Competition data: " + competition);
        return competitionsService.addCompetition(category, competition);
    }

    /**
     * PUT /football-pool/v1/api/competitions/{category}/{competitionId}
     * Update a competition (full update)
     */
    @PutMapping("/{category}/{competitionId}")
    public ResponseEntity<Document> updateCompetition(
            @PathVariable String category,
            @PathVariable String competitionId,
            @RequestBody Document updatedCompetition) {
        System.out.println("✏️ PUT request received: Update competition - Category: " + category + ", ID: " + competitionId);
        System.out.println("Updated data: " + updatedCompetition);
        return competitionsService.updateCompetition(category, competitionId, updatedCompetition);
    }

    /**
     * PATCH /football-pool/v1/api/competitions/{category}/{competitionId}
     * Patch a competition (partial update)
     * 
     * Only send the fields you want to update in the body
     */
    @PatchMapping("/{category}/{competitionId}")
    public ResponseEntity<Document> patchCompetition(
            @PathVariable String category,
            @PathVariable String competitionId,
            @RequestBody Map<String, Object> updates) {
        System.out.println("🔧 PATCH request received: Patch competition - Category: " + category + ", ID: " + competitionId);
        System.out.println("Updates: " + updates);
        return competitionsService.patchCompetition(category, competitionId, updates);
    }

    /**
     * DELETE /football-pool/v1/api/competitions/{category}/{competitionId}
     * Delete a competition from a category
     */
    @DeleteMapping("/{category}/{competitionId}")
    public ResponseEntity<Document> deleteCompetition(
            @PathVariable String category,
            @PathVariable String competitionId) {
        System.out.println("🗑️ DELETE request received: Delete competition - Category: " + category + ", ID: " + competitionId);
        return competitionsService.deleteCompetition(category, competitionId);
    }

    /**
     * GET /football-pool/v1/api/competitions/search?q={searchTerm}
     * Search competitions across all categories
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchCompetitions(@RequestParam("q") String searchTerm) {
        System.out.println("🔍 SEARCH request received: Search term - " + searchTerm);
        return competitionsService.searchCompetitions(searchTerm);
    }

    // ==================== QUALIFIED TEAMS ENDPOINTS ====================

    /**
     * GET /football-pool/v1/api/competitions/{category}/{competitionId}/teams
     * Get all qualified teams for a competition
     */
    @GetMapping("/{category}/{competitionId}/teams")
    public ResponseEntity<?> getQualifiedTeams(
            @PathVariable String category,
            @PathVariable String competitionId) {
        System.out.println("👥 GET request received: Get qualified teams - Category: " + category + ", Competition ID: " + competitionId);
        return competitionsService.getQualifiedTeams(category, competitionId);
    }

    /**
     * POST /football-pool/v1/api/competitions/{category}/{competitionId}/teams
     * Add a qualified team to a competition
     * 
     * Required fields in body:
     * - id (String)
     * - name (String)
     * 
     * Optional fields:
     * - country, flag, group, seed
     */
    @PostMapping("/{category}/{competitionId}/teams")
    public ResponseEntity<Document> addQualifiedTeam(
            @PathVariable String category,
            @PathVariable String competitionId,
            @RequestBody Document team) {
        System.out.println("➕ POST request received: Add team to competition - Category: " + category + ", Competition ID: " + competitionId);
        System.out.println("Team data: " + team);
        return competitionsService.addQualifiedTeam(category, competitionId, team);
    }

    /**
     * PUT /football-pool/v1/api/competitions/{category}/{competitionId}/teams/{teamId}
     * Update a qualified team in a competition (full update)
     */
    @PutMapping("/{category}/{competitionId}/teams/{teamId}")
    public ResponseEntity<Document> updateQualifiedTeam(
            @PathVariable String category,
            @PathVariable String competitionId,
            @PathVariable String teamId,
            @RequestBody Document updatedTeam) {
        System.out.println("✏️ PUT request received: Update team - Category: " + category + ", Competition ID: " + competitionId + ", Team ID: " + teamId);
        System.out.println("Updated team data: " + updatedTeam);
        return competitionsService.updateQualifiedTeam(category, competitionId, teamId, updatedTeam);
    }

    /**
     * DELETE /football-pool/v1/api/competitions/{category}/{competitionId}/teams/{teamId}
     * Delete a qualified team from a competition
     */
    @DeleteMapping("/{category}/{competitionId}/teams/{teamId}")
    public ResponseEntity<Document> deleteQualifiedTeam(
            @PathVariable String category,
            @PathVariable String competitionId,
            @PathVariable String teamId) {
        System.out.println("🗑️ DELETE request received: Delete team - Category: " + category + ", Competition ID: " + competitionId + ", Team ID: " + teamId);
        return competitionsService.deleteQualifiedTeam(category, competitionId, teamId);
    }
}


