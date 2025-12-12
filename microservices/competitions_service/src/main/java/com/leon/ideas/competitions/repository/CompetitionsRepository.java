package com.leon.ideas.competitions.repository;

import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class CompetitionsRepository {

    @Autowired
    private MongoTemplate competitionsMongoTemplate;

    private static final String COLLECTION_NAME = "competitions";

    /**
     * Get all competitions (entire document)
     * This method retrieves the first document from the competitions collection.
     * Since there should only be one document containing all competitions, we get the first one.
     */
    public Document getAllCompetitions() {
        try {
            // Get the first document from the collection (there should only be one)
            Query query = new Query();
            query.limit(1);
            List<Document> documents = competitionsMongoTemplate.find(query, Document.class, COLLECTION_NAME);
            
            if (documents != null && !documents.isEmpty()) {
                return documents.get(0);
            }
            
            System.err.println("⚠️ Warning: No competitions document found in collection");
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error retrieving competitions: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get the ObjectId of the competitions document
     * This is used by other methods that need to update the document
     */
    private ObjectId getCompetitionsDocumentId() {
        Document doc = getAllCompetitions();
        if (doc != null && doc.containsKey("_id")) {
            Object id = doc.get("_id");
            if (id instanceof ObjectId) {
                return (ObjectId) id;
            } else if (id instanceof String) {
                try {
                    return new ObjectId((String) id);
                } catch (IllegalArgumentException e) {
                    System.err.println("❌ Error: Invalid ObjectId format: " + id);
                }
            }
        }
        return null;
    }

    /**
     * Get competitions by category
     */
    public List<Document> getCompetitionsByCategory(String category) {
        Document allCompetitions = getAllCompetitions();
        if (allCompetitions != null && allCompetitions.containsKey(category)) {
            Object categoryData = allCompetitions.get(category);
            if (categoryData instanceof List) {
                return (List<Document>) categoryData;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Get a specific competition by category and ID
     */
    public Document getCompetitionById(String category, String competitionId) {
        List<Document> competitions = getCompetitionsByCategory(category);
        return competitions.stream()
                .filter(comp -> competitionId.equals(comp.getString("id")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add new competition to a category
     */
    public boolean addCompetition(String category, Document competition) {
        try {
            ObjectId objectId = getCompetitionsDocumentId();
            if (objectId == null) {
                System.err.println("❌ Error: Could not find competitions document");
                return false;
            }
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update().push(category, competition);
            
            UpdateResult result = competitionsMongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("❌ Error adding competition: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update a competition (PUT - full update)
     */
    public boolean updateCompetition(String category, String competitionId, Document updatedCompetition) {
        try {
            ObjectId objectId = getCompetitionsDocumentId();
            if (objectId == null) {
                System.err.println("❌ Error: Could not find competitions document");
                return false;
            }
            
            // First, get the index of the competition in the array
            List<Document> competitions = getCompetitionsByCategory(category);
            int index = -1;
            for (int i = 0; i < competitions.size(); i++) {
                if (competitionId.equals(competitions.get(i).getString("id"))) {
                    index = i;
                    break;
                }
            }
            
            if (index == -1) {
                return false;
            }
            
            // Update the competition at the specific index
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update().set(category + "." + index, updatedCompetition);
            
            UpdateResult result = competitionsMongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("❌ Error updating competition: " + e.getMessage());
            return false;
        }
    }

    /**
     * Patch a competition (PATCH - partial update)
     */
    public boolean patchCompetition(String category, String competitionId, Map<String, Object> updates) {
        try {
            ObjectId objectId = getCompetitionsDocumentId();
            if (objectId == null) {
                System.err.println("❌ Error: Could not find competitions document");
                return false;
            }
            
            // First, get the index of the competition in the array
            List<Document> competitions = getCompetitionsByCategory(category);
            int index = -1;
            for (int i = 0; i < competitions.size(); i++) {
                if (competitionId.equals(competitions.get(i).getString("id"))) {
                    index = i;
                    break;
                }
            }
            
            if (index == -1) {
                return false;
            }
            
            // Build update query for specific fields
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update();
            
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String fieldPath = category + "." + index + "." + entry.getKey();
                update.set(fieldPath, entry.getValue());
            }
            
            UpdateResult result = competitionsMongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("❌ Error patching competition: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a competition from a category
     */
    public boolean deleteCompetition(String category, String competitionId) {
        try {
            ObjectId objectId = getCompetitionsDocumentId();
            if (objectId == null) {
                System.err.println("❌ Error: Could not find competitions document");
                return false;
            }
            
            // Get the competition to delete
            Document competitionToDelete = getCompetitionById(category, competitionId);
            if (competitionToDelete == null) {
                return false;
            }
            
            // Remove the competition from the array
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update().pull(category, new Document("id", competitionId));
            
            UpdateResult result = competitionsMongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("❌ Error deleting competition: " + e.getMessage());
            return false;
        }
    }

    /**
     * Search competitions across all categories
     */
    public List<Document> searchCompetitions(String searchTerm) {
        List<Document> results = new ArrayList<>();
        Document allCompetitions = getAllCompetitions();
        
        if (allCompetitions == null) {
            return results;
        }
        
        String lowerSearchTerm = searchTerm.toLowerCase();
        
        // Search in all three categories
        String[] categories = {"fifaNationalTeamCups", "fifaOfficialClubCups", "nationalClubLeagues"};
        
        for (String category : categories) {
            List<Document> categoryCompetitions = getCompetitionsByCategory(category);
            for (Document comp : categoryCompetitions) {
                String name = comp.getString("name");
                String shortName = comp.getString("shortName");
                String country = comp.getString("country");
                
                if ((name != null && name.toLowerCase().contains(lowerSearchTerm)) ||
                    (shortName != null && shortName.toLowerCase().contains(lowerSearchTerm)) ||
                    (country != null && country.toLowerCase().contains(lowerSearchTerm))) {
                    
                    // Add category information to the result
                    comp.append("category", category);
                    results.add(comp);
                }
            }
        }
        
        return results;
    }

    /**
     * Add qualified team to a competition
     */
    public boolean addQualifiedTeam(String category, String competitionId, Document team) {
        try {
            ObjectId objectId = getCompetitionsDocumentId();
            if (objectId == null) {
                System.err.println("❌ Error: Could not find competitions document");
                return false;
            }
            
            // Find the competition index
            List<Document> competitions = getCompetitionsByCategory(category);
            int index = -1;
            for (int i = 0; i < competitions.size(); i++) {
                if (competitionId.equals(competitions.get(i).getString("id"))) {
                    index = i;
                    break;
                }
            }
            
            if (index == -1) {
                return false;
            }
            
            // Check if team already exists
            Document competition = competitions.get(index);
            List<Document> qualifiedTeams = (List<Document>) competition.get("qualifiedTeams");
            if (qualifiedTeams != null) {
                boolean teamExists = qualifiedTeams.stream()
                    .anyMatch(t -> team.getString("id").equals(t.getString("id")));
                if (teamExists) {
                    return false;
                }
            }
            
            // Add team to qualifiedTeams array
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update().push(category + "." + index + ".qualifiedTeams", team);
            
            UpdateResult result = competitionsMongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("❌ Error adding qualified team: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update a qualified team in a competition
     */
    public boolean updateQualifiedTeam(String category, String competitionId, String teamId, Document updatedTeam) {
        try {
            ObjectId objectId = getCompetitionsDocumentId();
            if (objectId == null) {
                System.err.println("❌ Error: Could not find competitions document");
                return false;
            }
            
            // Find the competition index
            List<Document> competitions = getCompetitionsByCategory(category);
            int competitionIndex = -1;
            for (int i = 0; i < competitions.size(); i++) {
                if (competitionId.equals(competitions.get(i).getString("id"))) {
                    competitionIndex = i;
                    break;
                }
            }
            
            if (competitionIndex == -1) {
                return false;
            }
            
            // Find the team index
            Document competition = competitions.get(competitionIndex);
            List<Document> qualifiedTeams = (List<Document>) competition.get("qualifiedTeams");
            if (qualifiedTeams == null) {
                return false;
            }
            
            int teamIndex = -1;
            for (int i = 0; i < qualifiedTeams.size(); i++) {
                if (teamId.equals(qualifiedTeams.get(i).getString("id"))) {
                    teamIndex = i;
                    break;
                }
            }
            
            if (teamIndex == -1) {
                return false;
            }
            
            // Update the team
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update().set(
                category + "." + competitionIndex + ".qualifiedTeams." + teamIndex,
                updatedTeam
            );
            
            UpdateResult result = competitionsMongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("❌ Error updating qualified team: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a qualified team from a competition
     */
    public boolean deleteQualifiedTeam(String category, String competitionId, String teamId) {
        try {
            ObjectId objectId = getCompetitionsDocumentId();
            if (objectId == null) {
                System.err.println("❌ Error: Could not find competitions document");
                return false;
            }
            
            // Find the competition index
            List<Document> competitions = getCompetitionsByCategory(category);
            int index = -1;
            for (int i = 0; i < competitions.size(); i++) {
                if (competitionId.equals(competitions.get(i).getString("id"))) {
                    index = i;
                    break;
                }
            }
            
            if (index == -1) {
                return false;
            }
            
            // Remove the team from qualifiedTeams array
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update().pull(
                category + "." + index + ".qualifiedTeams",
                new Document("id", teamId)
            );
            
            UpdateResult result = competitionsMongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("❌ Error deleting qualified team: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all qualified teams for a competition
     */
    public List<Document> getQualifiedTeams(String category, String competitionId) {
        Document competition = getCompetitionById(category, competitionId);
        if (competition != null && competition.containsKey("qualifiedTeams")) {
            Object teams = competition.get("qualifiedTeams");
            if (teams instanceof List) {
                return (List<Document>) teams;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Find a match in tournament structure by matchId
     */
    @SuppressWarnings("unchecked")
    public Document findMatchInTournamentStructure(String category, String competitionId, String matchId) {
        try {
            Document competition = getCompetitionById(category, competitionId);
            if (competition == null) {
                return null;
            }

            // First, try to find in tournamentStructure
            if (competition.containsKey("tournamentStructure")) {
                Document tournamentStructure = (Document) competition.get("tournamentStructure");
                if (tournamentStructure != null && tournamentStructure.containsKey("stages")) {
                    Map<String, Object> stages = (Map<String, Object>) tournamentStructure.get("stages");
                    if (stages != null) {
                        Document match = searchMatchInStages(stages, matchId);
                        if (match != null) {
                            return match;
                        }
                    }
                }
            }

            // If not found, try to find in groupsKindTournament
            if (competition.containsKey("groupsKindTournament")) {
                Document groupsKindTournament = (Document) competition.get("groupsKindTournament");
                if (groupsKindTournament != null && groupsKindTournament.containsKey("stages")) {
                    Map<String, Object> stages = (Map<String, Object>) groupsKindTournament.get("stages");
                    if (stages != null) {
                        Document match = searchMatchInStages(stages, matchId);
                        if (match != null) {
                            return match;
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("❌ Error finding match in tournament structure: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper method to search for a match in stages (used by both tournamentStructure and groupsKindTournament)
     */
    @SuppressWarnings("unchecked")
    private Document searchMatchInStages(Map<String, Object> stages, String matchId) {
        // Search through all stages
        for (Object stageObj : stages.values()) {
            if (!(stageObj instanceof Document)) {
                continue;
            }
            Document stage = (Document) stageObj;

            // Check matches in knockout stages
            if (stage.containsKey("matches")) {
                List<Document> matches = (List<Document>) stage.get("matches");
                if (matches != null) {
                    for (Document match : matches) {
                        if (matchId.equals(match.getString("matchId"))) {
                            return match;
                        }
                    }
                }
            }

            // Check matches in group stages
            if (stage.containsKey("groups")) {
                List<Document> groups = (List<Document>) stage.get("groups");
                if (groups != null) {
                    for (Document group : groups) {
                        if (group.containsKey("matches")) {
                            List<Document> matches = (List<Document>) group.get("matches");
                            if (matches != null) {
                                for (Document match : matches) {
                                    if (matchId.equals(match.getString("matchId"))) {
                                        return match;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Update match results in tournament structure
     */
    @SuppressWarnings("unchecked")
    public boolean updateMatchResults(String category, String competitionId, String matchId, Map<String, Object> results) {
        try {
            ObjectId objectId = getCompetitionsDocumentId();
            if (objectId == null) {
                System.err.println("❌ Error: Could not find competitions document");
                return false;
            }

            // Find the competition index
            List<Document> competitions = getCompetitionsByCategory(category);
            int competitionIndex = -1;
            for (int i = 0; i < competitions.size(); i++) {
                if (competitionId.equals(competitions.get(i).getString("id"))) {
                    competitionIndex = i;
                    break;
                }
            }

            if (competitionIndex == -1) {
                return false;
            }

            Document competition = competitions.get(competitionIndex);
            if (competition == null || !competition.containsKey("tournamentStructure")) {
                return false;
            }

            Document tournamentStructure = (Document) competition.get("tournamentStructure");
            if (tournamentStructure == null || !tournamentStructure.containsKey("stages")) {
                return false;
            }

            Map<String, Object> stages = (Map<String, Object>) tournamentStructure.get("stages");
            if (stages == null) {
                return false;
            }

            // Find and update the match
            String matchPath = null;
            for (Map.Entry<String, Object> stageEntry : stages.entrySet()) {
                if (!(stageEntry.getValue() instanceof Document)) {
                    continue;
                }
                Document stage = (Document) stageEntry.getValue();

                // Check matches in knockout stages
                if (stage.containsKey("matches")) {
                    List<Document> matches = (List<Document>) stage.get("matches");
                    if (matches != null) {
                        for (int i = 0; i < matches.size(); i++) {
                            Document match = matches.get(i);
                            if (matchId.equals(match.getString("matchId"))) {
                                matchPath = category + "." + competitionIndex + ".tournamentStructure.stages." + stageEntry.getKey() + ".matches." + i;
                                break;
                            }
                        }
                    }
                }

                // Check matches in group stages
                if (stage.containsKey("groups")) {
                    List<Document> groups = (List<Document>) stage.get("groups");
                    if (groups != null) {
                        for (int g = 0; g < groups.size(); g++) {
                            Document group = groups.get(g);
                            if (group.containsKey("matches")) {
                                List<Document> matches = (List<Document>) group.get("matches");
                                if (matches != null) {
                                    for (int m = 0; m < matches.size(); m++) {
                                        Document match = matches.get(m);
                                        if (matchId.equals(match.getString("matchId"))) {
                                            matchPath = category + "." + competitionIndex + ".tournamentStructure.stages." + stageEntry.getKey() + ".groups." + g + ".matches." + m;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (matchPath != null) {
                    break;
                }
            }

            if (matchPath == null) {
                return false;
            }

            // Update the match with new results
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update();
            
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                update.set(matchPath + "." + entry.getKey(), entry.getValue());
            }

            UpdateResult result = competitionsMongoTemplate.updateFirst(query, update, COLLECTION_NAME);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("❌ Error updating match results: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}


