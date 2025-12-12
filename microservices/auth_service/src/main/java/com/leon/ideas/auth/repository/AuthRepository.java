package com.leon.ideas.auth.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

@Repository
public class AuthRepository {

    private final MongoTemplate usersMongoTemplate;

    @Autowired
    public AuthRepository(@Qualifier("usersMongoTemplate") MongoTemplate usersMongoTemplate) {
        this.usersMongoTemplate = usersMongoTemplate;
    }

    public List<Document> findAllUsers() {
        return usersMongoTemplate.findAll(Document.class, "users");
    }

    public boolean findById(String userId) {
        Document user = usersMongoTemplate.findById(userId, Document.class, "users");
        return user != null;
    }

    public Document findUserByEmail(String email) {
        Query query = new Query(Criteria.where("email").regex("^" + email + "$", "i"));
        return usersMongoTemplate.findOne(query, Document.class, "users");
    }

    public void saveUser(Document user) {
        usersMongoTemplate.save(user, "users");
    }
    
    /**
     * Add a groupId to the user's "groups" array (if not already present).
     */
    public void addGroupToUser(String userId, String groupId) {
        if (userId == null || groupId == null) {
            return;
        }
        
        try {
            ObjectId objectId = new ObjectId(userId);
            Query query = new Query(Criteria.where("_id").is(objectId));
            Update update = new Update().addToSet("groups", groupId);
            usersMongoTemplate.updateFirst(query, update, "users");
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format when adding group to user: " + userId);
        }
    }
    
    public void saveResetCode(String email, String code) {
        Query deleteQuery = new Query(Criteria.where("email").is(email));
        usersMongoTemplate.remove(deleteQuery, "password_reset_codes");
        
        Document doc = new Document();
        doc.put("email", email);
        doc.put("code", code);
        doc.put("createdAt", System.currentTimeMillis());
        usersMongoTemplate.save(doc, "password_reset_codes");
    }
    
    public Document findResetCode(String email, String code) {
        Query query = new Query(
            Criteria.where("email").is(email)
                .and("code").is(code)
        );
        return usersMongoTemplate.findOne(query, Document.class, "password_reset_codes");
    }
    
    public void deleteResetCode(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        usersMongoTemplate.remove(query, "password_reset_codes");
    }
    
    public void deleteExpiredCodes() {
        long thirtyMinutesAgo = System.currentTimeMillis() - 30 * 60 * 1000;
        Query query = new Query(Criteria.where("createdAt").lt(thirtyMinutesAgo));
        usersMongoTemplate.remove(query, "password_reset_codes");
    }
    
    public void updateUserPassword(String email, String newPassword) {
        Document user = findUserByEmail(email);
        if (user != null) {
            @SuppressWarnings("unchecked")
            List<String> passwords = (List<String>) user.get("passwords");
            if (passwords == null) {
                passwords = new ArrayList<>();
            }
            // Agregar la nueva contraseña al inicio
            passwords.add(0, newPassword);
            // Mantener solo las últimas 5 contraseñas
            if (passwords.size() > 5) {
                passwords = passwords.subList(0, 5);
            }
            user.put("passwords", passwords);
            saveUser(user);
        }
    }

    public Document findUserById(String userId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            return usersMongoTemplate.findById(objectId, Document.class, "users");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void deleteUser(String userId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            usersMongoTemplate.remove(new Query(Criteria.where("_id").is(objectId)), "users");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format");
        }
    }
    
    /**
     * Save or update a match prediction for a user.
     * New structure: predictions is a Document with competitionId as keys:
     * {
     *   "club-world-cup": {
     *     matchInfo: [
     *       {
     *         matchId: String,
     *         team1: String,
     *         team2: String,
     *         team1Score: Integer,
     *         team2Score: Integer,
     *         realTeam1Score: Integer,
     *         realTeam2Score: Integer,
     *         extraTime: Boolean,
     *         realExtraTime: Boolean,
     *         penaltiesteam1Score: Integer | null,
     *         penaltiesteam2Score: Integer | null,
     *         predictedDate: Date
     *       }
     *     ],
     *     points: Integer (accumulated points for all matches in this competition)
     *   }
     * }
     * 
     * Note: Prediction is saved ONCE per user per match per competition (not per group)
     */
    public void savePrediction(String userId, String competitionId, String matchId, 
                               Integer team1Score, Integer team2Score, 
                               Integer realTeam1Score, Integer realTeam2Score,
                               Boolean extraTime, Boolean realExtraTime,
                               Integer penaltiesteam1Score, Integer penaltiesteam2Score,
                               String team1, String team2) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Query query = new Query(Criteria.where("_id").is(objectId));
            
            Document user = usersMongoTemplate.findOne(query, Document.class, "users");
            if (user == null) {
                System.err.println("❌ User not found: " + userId);
                return;
            }
            
            // Get predictions - handle both old structure (List) and new structure (Document)
            Object predictionsObj = user.get("predictions");
            Document predictions = null;
            
            if (predictionsObj == null) {
                // No predictions exist, create new structure
                predictions = new Document();
            } else if (predictionsObj instanceof Document) {
                // Already in new structure
                predictions = (Document) predictionsObj;
            } else if (predictionsObj instanceof List) {
                // Old structure (List) - migrate to new structure
                System.out.println("🔄 Migrating predictions from old structure (List) to new structure (Document)");
                @SuppressWarnings("unchecked")
                List<Document> oldPredictions = (List<Document>) predictionsObj;
                predictions = migratePredictionsToNewStructure(oldPredictions);
            } else {
                // Unknown structure, create new
                System.out.println("⚠️ Unknown predictions structure, creating new structure");
                predictions = new Document();
            }
            
            // Get or create competition prediction object
            @SuppressWarnings("unchecked")
            Document competitionPrediction = (Document) predictions.get(competitionId);
            if (competitionPrediction == null) {
                competitionPrediction = new Document();
                competitionPrediction.put("matchInfo", new ArrayList<Document>());
                competitionPrediction.put("points", 0);
            }
            
            @SuppressWarnings("unchecked")
            List<Document> matchInfo = (List<Document>) competitionPrediction.get("matchInfo");
            if (matchInfo == null) {
                matchInfo = new ArrayList<>();
            }
            
            // Remove existing match if exists
            matchInfo.removeIf(match -> matchId.equals(match.getString("matchId")));
            
            // Create new match info document
            Document matchDoc = new Document();
            matchDoc.put("matchId", matchId);
            matchDoc.put("team1", team1 != null ? team1 : "");
            matchDoc.put("team2", team2 != null ? team2 : "");
            matchDoc.put("team1Score", team1Score != null ? team1Score : 0);
            matchDoc.put("team2Score", team2Score != null ? team2Score : 0);
            matchDoc.put("realTeam1Score", realTeam1Score != null ? realTeam1Score : 0);
            matchDoc.put("realTeam2Score", realTeam2Score != null ? realTeam2Score : 0);
            if (extraTime != null) {
                matchDoc.put("extraTime", extraTime);
            }
            if (realExtraTime != null) {
                matchDoc.put("realExtraTime", realExtraTime);
            }
            if (penaltiesteam1Score != null) {
                matchDoc.put("penaltiesteam1Score", penaltiesteam1Score);
            }
            if (penaltiesteam2Score != null) {
                matchDoc.put("penaltiesteam2Score", penaltiesteam2Score);
            }
            matchDoc.put("predictedDate", new java.util.Date());
            
            matchInfo.add(matchDoc);
            
            // Sort matchInfo by matchId
            matchInfo.sort((m1, m2) -> {
                String id1 = m1.getString("matchId");
                String id2 = m2.getString("matchId");
                if (id1 == null) id1 = "";
                if (id2 == null) id2 = "";
                return id1.compareTo(id2);
            });
            
            // Calculate points for this match and update competition points
            int matchPoints = calculateMatchPoints(matchDoc);
            int currentPoints = competitionPrediction.getInteger("points", 0);
            // Remove old points for this match if it existed (we'll recalculate all)
            // For now, we'll recalculate all points from scratch
            int totalPoints = recalculateCompetitionPoints(matchInfo);
            competitionPrediction.put("points", totalPoints);
            competitionPrediction.put("matchInfo", matchInfo);
            
            // Update predictions document
            predictions.put(competitionId, competitionPrediction);
            
            // Recalculate groupScores for ALL groups based on all predictions
            recalculateGroupScoresFromPredictions(user, predictions);
            
            Update update = new Update()
                .set("predictions", predictions)
                .set("groupScores", user.get("groupScores"));
            usersMongoTemplate.updateFirst(query, update, "users");
            
            System.out.println("✅ Prediction saved for user " + userId + ", competition " + competitionId + ", match " + matchId + ", points: " + matchPoints);
            System.out.println("📊 Structure: predictions[" + competitionId + "] = { matchInfo: " + matchInfo.size() + " matches, points: " + totalPoints + " }");
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format when saving prediction: " + userId);
        } catch (Exception e) {
            System.err.println("❌ Error saving prediction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calculate points for a single match based on prediction vs real result
     */
    private int calculateMatchPoints(Document matchDoc) {
        int points = 0;
        
        Integer team1Score = matchDoc.getInteger("team1Score", 0);
        Integer team2Score = matchDoc.getInteger("team2Score", 0);
        Integer realTeam1Score = matchDoc.getInteger("realTeam1Score", 0);
        Integer realTeam2Score = matchDoc.getInteger("realTeam2Score", 0);
        
        // Check for exact score match
        if (team1Score.equals(realTeam1Score) && team2Score.equals(realTeam2Score)) {
            points = 5; // Exact score: 5 points
        } else {
            // Check for correct result (win/draw/loss)
            String predictedResult;
            if (team1Score > team2Score) {
                predictedResult = "team1_win";
            } else if (team2Score > team1Score) {
                predictedResult = "team2_win";
            } else {
                predictedResult = "draw";
            }
            
            String actualResult;
            if (realTeam1Score > realTeam2Score) {
                actualResult = "team1_win";
            } else if (realTeam2Score > realTeam1Score) {
                actualResult = "team2_win";
            } else {
                actualResult = "draw";
            }
            
            if (predictedResult.equals(actualResult)) {
                points = 3; // Correct result without exact score: 3 points
            } else {
                points = 0; // Wrong result: 0 points
            }
        }
        
        // Additional points for extra time
        Boolean extraTime = matchDoc.getBoolean("extraTime");
        Boolean realExtraTime = matchDoc.getBoolean("realExtraTime");
        if (extraTime != null && extraTime && realExtraTime != null && realExtraTime) {
            points += 1; // +1 point for correct extra time prediction
        }
        
        // Additional points for penalties
        // Note: We need real penalty scores to compare. For now, if user predicted penalties
        // and the match ended in a draw (which would require penalties), we give +2 points
        // If we have real penalty scores, we can compare for exact match (+3 additional points)
        Integer penaltiesteam1Score = matchDoc.getInteger("penaltiesteam1Score");
        Integer penaltiesteam2Score = matchDoc.getInteger("penaltiesteam2Score");
        
        // Check if match ended in draw (which would require penalties in knockout stages)
        if (realTeam1Score.equals(realTeam2Score) && penaltiesteam1Score != null && penaltiesteam2Score != null) {
            // User predicted penalties and match went to penalties (draw)
            points += 2; // +2 points for correct penalties prediction
            
            // TODO: If we receive real penalty scores, compare them here for exact match (+3 additional points)
            // For now, we assume if user predicted penalties and match went to penalties, it's correct
        }
        
        return points;
    }
    
    /**
     * Recalculate total points for a competition by summing all match points
     */
    private int recalculateCompetitionPoints(List<Document> matchInfo) {
        int totalPoints = 0;
        for (Document match : matchInfo) {
            totalPoints += calculateMatchPoints(match);
        }
        return totalPoints;
    }
    
    /**
     * Migrate old predictions structure (List) to new structure (Document by competitionId)
     * This handles the migration from the old format to the new format
     */
    private Document migratePredictionsToNewStructure(List<Document> oldPredictions) {
        Document newPredictions = new Document();
        
        if (oldPredictions == null || oldPredictions.isEmpty()) {
            return newPredictions;
        }
        
        // For each old prediction, we need to determine its competitionId
        // Since old predictions don't have competitionId, we'll need to group them
        // For now, we'll put them in a default competition or try to infer from matchId
        // This is a temporary migration - ideally we'd have competitionId in old data
        
        // Group by a default competitionId (you may need to adjust this logic)
        String defaultCompetitionId = "migrated-predictions";
        Document competitionPrediction = new Document();
        List<Document> matchInfo = new ArrayList<>();
        int totalPoints = 0;
        
        for (Document oldPred : oldPredictions) {
            Document matchDoc = new Document();
            matchDoc.put("matchId", oldPred.getString("matchId"));
            matchDoc.put("team1Score", oldPred.getInteger("team1Score", 0));
            matchDoc.put("team2Score", oldPred.getInteger("team2Score", 0));
            
            // Old structure doesn't have real scores, set to 0
            matchDoc.put("realTeam1Score", 0);
            matchDoc.put("realTeam2Score", 0);
            
            // Copy optional fields if they exist
            if (oldPred.containsKey("userExtraTime")) {
                matchDoc.put("extraTime", oldPred.get("userExtraTime"));
            }
            if (oldPred.containsKey("userPenalties")) {
                // Old structure might have userPenalties as boolean
                Boolean hasPenalties = oldPred.getBoolean("userPenalties");
                if (hasPenalties != null && hasPenalties) {
                    // If penalties were predicted, try to get scores
                    Integer pen1 = oldPred.getInteger("userPenaltiesTeam1Score");
                    Integer pen2 = oldPred.getInteger("userPenaltiesTeam2Score");
                    if (pen1 != null) matchDoc.put("penaltiesteam1Score", pen1);
                    if (pen2 != null) matchDoc.put("penaltiesteam2Score", pen2);
                }
            }
            
            if (oldPred.containsKey("predictedDate")) {
                matchDoc.put("predictedDate", oldPred.get("predictedDate"));
            } else {
                matchDoc.put("predictedDate", new java.util.Date());
            }
            
            // Calculate points (will be 0 since real scores are 0)
            int matchPoints = calculateMatchPoints(matchDoc);
            totalPoints += matchPoints;
            
            matchInfo.add(matchDoc);
        }
        
        competitionPrediction.put("matchInfo", matchInfo);
        competitionPrediction.put("points", totalPoints);
        newPredictions.put(defaultCompetitionId, competitionPrediction);
        
        System.out.println("✅ Migrated " + oldPredictions.size() + " predictions to new structure");
        return newPredictions;
    }
    
    
    /**
     * Update points for a specific prediction by recalculating from match info
     * Points are automatically recalculated when match info is updated
     */
    public void updatePredictionPoints(String userId, String competitionId, String matchId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Query query = new Query(Criteria.where("_id").is(objectId));
            
            Document user = usersMongoTemplate.findOne(query, Document.class, "users");
            if (user == null) {
                System.err.println("❌ User not found: " + userId);
                return;
            }
            
            @SuppressWarnings("unchecked")
            Document predictions = (Document) user.get("predictions");
            if (predictions == null) {
                return;
            }
            
            @SuppressWarnings("unchecked")
            Document competitionPrediction = (Document) predictions.get(competitionId);
            if (competitionPrediction == null) {
                return;
            }
            
            @SuppressWarnings("unchecked")
            List<Document> matchInfo = (List<Document>) competitionPrediction.get("matchInfo");
            if (matchInfo == null) {
                return;
            }
            
            // Recalculate points for the competition
            int totalPoints = recalculateCompetitionPoints(matchInfo);
            competitionPrediction.put("points", totalPoints);
            competitionPrediction.put("matchInfo", matchInfo);
            predictions.put(competitionId, competitionPrediction);
            
            // Recalculate groupScores for ALL groups based on all predictions
            recalculateGroupScoresFromPredictions(user, predictions);
            
            Update update = new Update()
                .set("predictions", predictions)
                .set("groupScores", user.get("groupScores"));
            usersMongoTemplate.updateFirst(query, update, "users");
            
            System.out.println("✅ Prediction points updated for user " + userId + ", competition " + competitionId + ", match " + matchId);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format when updating prediction points: " + userId);
        } catch (Exception e) {
            System.err.println("❌ Error updating prediction points: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Recalculate groupScores for all groups based on predictions
     * This method sums up all points from competitions that match each group's competitionId
     */
    private void recalculateGroupScoresFromPredictions(Document user, Document predictions) {
        try {
            // Get user's groups - we need to get group info to know competitionId
            @SuppressWarnings("unchecked")
            List<String> userGroups = (List<String>) user.get("groups");
            if (userGroups == null || userGroups.isEmpty()) {
                return;
            }
            
            // Initialize groupScores map
            Document groupScores = new Document();
            
            // For each group, we need to get its competitionId and sum points from that competition
            // Note: This requires a call to groups_service, but for now we'll use a simpler approach
            // The groups_service will handle updating the user's score in the group document
            // Here we just maintain a local cache of scores per group
            
            // For now, we'll set all groups to 0 and let groups_service update them
            // This is a temporary solution until we can get competitionId from groups
            for (String groupId : userGroups) {
                groupScores.put(groupId, 0);
            }
            
            user.put("groupScores", groupScores);
            System.out.println("✅ GroupScores recalculated: " + groupScores);
        } catch (Exception e) {
            System.err.println("⚠️ Error recalculating groupScores: " + e.getMessage());
        }
    }
    
    /**
     * Recalculate groupScores for all groups based on predictions (OLD METHOD - DEPRECATED)
     * This method sums up all points for each group
     */
    @Deprecated
    private void recalculateGroupScores(Document user, List<Document> predictions) {
        try {
            // Get user's groups
            @SuppressWarnings("unchecked")
            List<String> userGroups = (List<String>) user.get("groups");
            if (userGroups == null || userGroups.isEmpty()) {
                return;
            }
            
            // Initialize groupScores map
            Document groupScores = new Document();
            
            // For each group, sum all points from predictions
            for (String groupId : userGroups) {
                int totalScore = 0;
                for (Document pred : predictions) {
                    Object pointsObj = pred.get("points");
                    if (pointsObj != null) {
                        int predPoints = pointsObj instanceof Integer ? (Integer) pointsObj : 
                                       (pointsObj instanceof Number ? ((Number) pointsObj).intValue() : 0);
                        totalScore += predPoints;
                    }
                }
                groupScores.put(groupId, totalScore);
            }
            
            user.put("groupScores", groupScores);
            System.out.println("✅ GroupScores recalculated: " + groupScores);
        } catch (Exception e) {
            System.err.println("⚠️ Error recalculating groupScores: " + e.getMessage());
        }
    }
    
    /**
     * Get total points for a competition
     * Handles migration from old structure if needed
     */
    public Integer getCompetitionPoints(String userId, String competitionId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Document user = usersMongoTemplate.findById(objectId, Document.class, "users");
            if (user == null) {
                return 0;
            }
            
            Object predictionsObj = user.get("predictions");
            Document predictions = null;
            
            if (predictionsObj == null) {
                return 0;
            } else if (predictionsObj instanceof Document) {
                predictions = (Document) predictionsObj;
            } else if (predictionsObj instanceof List) {
                // Old structure - migrate it
                @SuppressWarnings("unchecked")
                List<Document> oldPredictions = (List<Document>) predictionsObj;
                predictions = migratePredictionsToNewStructure(oldPredictions);
                
                // Save migrated structure
                Update update = new Update().set("predictions", predictions);
                Query query = new Query(Criteria.where("_id").is(objectId));
                usersMongoTemplate.updateFirst(query, update, "users");
            } else {
                return 0;
            }
            
            @SuppressWarnings("unchecked")
            Document competitionPrediction = (Document) predictions.get(competitionId);
            if (competitionPrediction == null) {
                return 0;
            }
            
            Object pointsObj = competitionPrediction.get("points");
            if (pointsObj == null) {
                return 0;
            }
            
            if (pointsObj instanceof Integer) {
                return (Integer) pointsObj;
            } else if (pointsObj instanceof Number) {
                return ((Number) pointsObj).intValue();
            }
            
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format when getting competition points: " + userId);
            return 0;
        } catch (Exception e) {
            System.err.println("❌ Error getting competition points: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get total score for a user in a specific group from the groupScores map
     */
    public Integer getTotalScoreForGroup(String userId, String groupId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Document user = usersMongoTemplate.findById(objectId, Document.class, "users");
            if (user == null) {
                return 0;
            }
            
            @SuppressWarnings("unchecked")
            Document groupScores = (Document) user.get("groupScores");
            if (groupScores == null) {
                return 0;
            }
            
            Object scoreObj = groupScores.get(groupId);
            if (scoreObj == null) {
                return 0;
            }
            
            if (scoreObj instanceof Integer) {
                return (Integer) scoreObj;
            } else if (scoreObj instanceof Number) {
                return ((Number) scoreObj).intValue();
            }
            
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format when getting total score: " + userId);
            return 0;
        } catch (Exception e) {
            System.err.println("❌ Error getting total score: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get all predictions for a user
     * Returns the predictions Document with competitionId as keys
     * If groupId is provided, we need to get the competitionId from the group to filter
     * Handles migration from old structure (List) to new structure (Document)
     */
    public Document getUserPredictions(String userId, String groupId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Document user = usersMongoTemplate.findById(objectId, Document.class, "users");
            
            if (user == null) {
                System.out.println("⚠️ User not found: " + userId);
                return new Document();
            }
            
            Object predictionsObj = user.get("predictions");
            Document allPredictions = null;
            
            if (predictionsObj == null) {
                System.out.println("ℹ️ User has no predictions: " + userId);
                return new Document();
            } else if (predictionsObj instanceof Document) {
                // Already in new structure
                allPredictions = (Document) predictionsObj;
            } else if (predictionsObj instanceof List) {
                // Old structure - migrate it
                System.out.println("🔄 Migrating predictions from old structure to new structure for user: " + userId);
                @SuppressWarnings("unchecked")
                List<Document> oldPredictions = (List<Document>) predictionsObj;
                allPredictions = migratePredictionsToNewStructure(oldPredictions);
                
                // Save the migrated structure back to database
                Update update = new Update().set("predictions", allPredictions);
                Query query = new Query(Criteria.where("_id").is(objectId));
                usersMongoTemplate.updateFirst(query, update, "users");
                System.out.println("✅ Predictions migrated and saved for user: " + userId);
            } else {
                System.out.println("⚠️ Unknown predictions structure for user: " + userId);
                return new Document();
            }
            
            // If no groupId filter, return all predictions
            if (groupId == null || groupId.trim().isEmpty()) {
                return allPredictions;
            }
            
            // If groupId is provided, we would need to get the competitionId from the group
            // For now, return all predictions (the frontend can filter by competitionId)
            // TODO: Get competitionId from group and filter by it
            return allPredictions;
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format: " + userId + " - " + e.getMessage());
            return new Document();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error getting predictions for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return new Document();
        }
    }
    
    /**
     * Get a specific prediction for a user, competition, and match
     */
    public Document getUserPrediction(String userId, String competitionId, String matchId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Document user = usersMongoTemplate.findById(objectId, Document.class, "users");
            
            if (user == null) {
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Document predictions = (Document) user.get("predictions");
            if (predictions == null) {
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Document competitionPrediction = (Document) predictions.get(competitionId);
            if (competitionPrediction == null) {
                return null;
            }
            
            @SuppressWarnings("unchecked")
            List<Document> matchInfo = (List<Document>) competitionPrediction.get("matchInfo");
            if (matchInfo == null) {
                return null;
            }
            
            // Find the match
            for (Document match : matchInfo) {
                if (matchId.equals(match.getString("matchId"))) {
                    return match;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error getting user prediction: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get all group IDs that a user belongs to
     */
    @SuppressWarnings("unchecked")
    public List<String> getUserGroups(String userId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Document user = usersMongoTemplate.findById(objectId, Document.class, "users");
            if (user == null) {
                return new ArrayList<>();
            }
            
            List<String> groups = (List<String>) user.get("groups");
            if (groups == null) {
                return new ArrayList<>();
            }
            
            return groups;
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format when getting user groups: " + userId);
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error getting user groups: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}