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
     * Structure: predictions array contains documents with:
     * - groupId: String
     * - matchId: String
     * - team1Score: Integer
     * - team2Score: Integer
     * - predictedDate: Date
     * - points: Integer (calculated later)
     */
    public void savePrediction(String userId, String groupId, String matchId, Integer team1Score, Integer team2Score) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Query query = new Query(Criteria.where("_id").is(objectId));
            
            Document user = usersMongoTemplate.findOne(query, Document.class, "users");
            if (user == null) {
                System.err.println("❌ User not found: " + userId);
                return;
            }
            
            @SuppressWarnings("unchecked")
            List<Document> predictions = (List<Document>) user.get("predictions");
            if (predictions == null) {
                predictions = new ArrayList<>();
            }
            
            // Remove existing prediction for this match if exists
            predictions.removeIf(pred -> 
                groupId.equals(pred.getString("groupId")) && 
                matchId.equals(pred.getString("matchId"))
            );
            
            // Add new prediction
            Document prediction = new Document();
            prediction.put("groupId", groupId);
            prediction.put("matchId", matchId);
            prediction.put("team1Score", team1Score);
            prediction.put("team2Score", team2Score);
            prediction.put("predictedDate", new java.util.Date());
            prediction.put("points", 0); // Will be calculated later
            
            predictions.add(prediction);
            
            Update update = new Update().set("predictions", predictions);
            usersMongoTemplate.updateFirst(query, update, "users");
            
            System.out.println("✅ Prediction saved for user " + userId + ", group " + groupId + ", match " + matchId);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format when saving prediction: " + userId);
        } catch (Exception e) {
            System.err.println("❌ Error saving prediction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get all predictions for a user, optionally filtered by groupId
     */
    public List<Document> getUserPredictions(String userId, String groupId) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Document user = usersMongoTemplate.findById(objectId, Document.class, "users");
            
            if (user == null) {
                return new ArrayList<>();
            }
            
            @SuppressWarnings("unchecked")
            List<Document> allPredictions = (List<Document>) user.get("predictions");
            if (allPredictions == null) {
                return new ArrayList<>();
            }
            
            if (groupId == null || groupId.trim().isEmpty()) {
                return allPredictions;
            }
            
            // Filter by groupId
            List<Document> filtered = new ArrayList<>();
            for (Document pred : allPredictions) {
                if (groupId.equals(pred.getString("groupId"))) {
                    filtered.add(pred);
                }
            }
            
            return filtered;
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Invalid user ID format: " + userId);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get a specific prediction for a user, group, and match
     */
    public Document getUserPrediction(String userId, String groupId, String matchId) {
        List<Document> predictions = getUserPredictions(userId, groupId);
        for (Document pred : predictions) {
            if (matchId.equals(pred.getString("matchId"))) {
                return pred;
            }
        }
        return null;
    }
    
    /**
     * Update prediction points after calculating scores
     */
    public void updatePredictionPoints(String userId, String groupId, String matchId, Integer points) {
        try {
            ObjectId objectId = new ObjectId(userId);
            Query query = new Query(Criteria.where("_id").is(objectId));
            
            Document user = usersMongoTemplate.findOne(query, Document.class, "users");
            if (user == null) {
                return;
            }
            
            @SuppressWarnings("unchecked")
            List<Document> predictions = (List<Document>) user.get("predictions");
            if (predictions == null) {
                return;
            }
            
            // Find and update the prediction
            for (Document pred : predictions) {
                if (groupId.equals(pred.getString("groupId")) && 
                    matchId.equals(pred.getString("matchId"))) {
                    pred.put("points", points);
                    break;
                }
            }
            
            Update update = new Update().set("predictions", predictions);
            usersMongoTemplate.updateFirst(query, update, "users");
        } catch (Exception e) {
            System.err.println("❌ Error updating prediction points: " + e.getMessage());
        }
    }
}