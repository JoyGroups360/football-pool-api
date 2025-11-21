package com.leon.ideas.competitions.repository;

import com.mongodb.client.result.DeleteResult;
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
    private static final String DOCUMENT_ID = "6913741bb6e91976e74b53d3";

    /**
     * Get all competitions (entire document)
     */
    public Document getAllCompetitions() {
        try {
            ObjectId objectId = new ObjectId(DOCUMENT_ID);
            return competitionsMongoTemplate.findById(objectId, Document.class, COLLECTION_NAME);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error: Invalid ObjectId format for document ID");
            return null;
        }
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
            ObjectId objectId = new ObjectId(DOCUMENT_ID);
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
            ObjectId objectId = new ObjectId(DOCUMENT_ID);
            
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
            ObjectId objectId = new ObjectId(DOCUMENT_ID);
            
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
            ObjectId objectId = new ObjectId(DOCUMENT_ID);
            
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
            ObjectId objectId = new ObjectId(DOCUMENT_ID);
            
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
            ObjectId objectId = new ObjectId(DOCUMENT_ID);
            
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
            ObjectId objectId = new ObjectId(DOCUMENT_ID);
            
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
}


