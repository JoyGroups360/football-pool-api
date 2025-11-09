package com.leon.ideas.auth.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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
}