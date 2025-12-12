package com.leon.ideas.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.leon.ideas.auth.repository.AuthRepository;

@Service
public class AuthService {
    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private MailService mailService;
    
    @Autowired
    private GmailMailService gmailMailService;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private FacebookAuthService facebookAuthService;
    
    @Autowired
    private GroupsClientService groupsClientService;
    
    @Value("${service.token}")
    private String serviceToken;
    
    public String getServiceToken() {
        return serviceToken;
    }

    public ResponseEntity<Document> authenticateUser(String email, String password) {
        try {
            Document user = authRepository.findUserByEmail(email);
            if (user == null) {
                Document error = new Document("error", "User not found");
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }

            @SuppressWarnings("unchecked")
            List<String> passwords = (List<String>) user.get("passwords");

            if (passwords == null || passwords.isEmpty()) {
                Document error = new Document("error", "User has no registered passwords");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            String currentPassword = passwords.get(0);

            if (currentPassword.equals(password)) {
                convertIdToString(user);
                
                // Generate JWT tokens
                String userId = user.getString("_id");
                String accessToken = jwtService.generateToken(email, userId);
                String refreshToken = jwtService.generateRefreshToken(email, userId);
                
                // Add tokens to response
                Document response = new Document();
                response.putAll(user);
                response.put("accessToken", accessToken);
                response.put("refreshToken", refreshToken);
                response.put("tokenType", "Bearer");
                response.put("expiresIn", 86400); // 24 hours in seconds
                
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (Exception e) {
            Document error = new Document("error", "Authentication failed: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Document errorResponse = new Document("error", "Incorrect password");
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    public boolean userExistence(String userId) {
        return authRepository.findById(userId);
    }

    /**
     * Add a groupId to the user's groups array.
     */
    public ResponseEntity<Document> addGroupToUser(String userId, String groupId) {
        if (userId == null || userId.trim().isEmpty() || groupId == null || groupId.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "User ID and group ID are required"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (!userId.matches("^[0-9a-fA-F]{24}$")) {
            return new ResponseEntity<>(
                new Document("error", "Invalid user ID format"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        Document user = authRepository.findUserById(userId);
        if (user == null) {
            return new ResponseEntity<>(
                new Document("error", "User not found"),
                HttpStatus.NOT_FOUND
            );
        }
        
        authRepository.addGroupToUser(userId, groupId);
        
        Document response = new Document();
        response.put("message", "Group added to user successfully");
        response.put("userId", userId);
        response.put("groupId", groupId);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public List<Document> getAllUsers() {
        try {
            System.out.println("🔍 DEBUG - Getting all users...");
            List<Document> users = authRepository.findAllUsers();
            System.out.println("🔍 DEBUG - Found " + (users != null ? users.size() : 0) + " users");
            
            if (users == null || users.isEmpty()) {
                System.out.println("🔍 DEBUG - No users found, returning empty list");
                return new ArrayList<>();
            }

            System.out.println("🔍 DEBUG - Processing users...");
            users.forEach(user -> {
                try {
                    convertIdToString(user);
                    user.remove("passwords");
                    System.out.println("🔍 DEBUG - Processed user: " + user.getString("email"));
                } catch (Exception e) {
                    System.err.println("Error processing user: " + e.getMessage());
                }
            });

            System.out.println("✅ DEBUG - Successfully processed all users");
            return users;
        } catch (Exception e) {
            System.err.println("❌ ERROR in getAllUsers: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public ResponseEntity<Document> createUser(Document body) {
        String email = body.getString("email");
        String password = body.getString("password");
        String confirmPassword = body.getString("confirmPassword");

        if (email == null || password == null || confirmPassword == null) {
            Document error = new Document("error", "Email and passwords are required");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        if (!password.equals(confirmPassword)) {
            Document error = new Document("error", "Passwords do not match");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        Document existingUser = authRepository.findUserByEmail(email);
        if (existingUser != null) {
            Document error = new Document("error", "User already exists");
            return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }
        
        if (password.length() < 6) {
            Document error = new Document("error", "Password must be at least 6 characters");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        List<String> passwords = new ArrayList<>();
        passwords.add(0, password);

        Document newUser = new Document();
        newUser.put("email", email);
        newUser.put("passwords", passwords);
        newUser.put("name", body.getString("name"));
        newUser.put("lastName", body.getString("lastName"));
        newUser.put("birth", body.getString("birth"));
        newUser.put("country", body.getString("country"));
        newUser.put("state", body.getString("state"));
        newUser.put("city", body.getString("city"));
        newUser.put("phone", body.getString("phone"));
        newUser.put("zipcode", body.getString("zipcode"));
        newUser.put("preferredTeams", body.get("preferredTeams", List.class));
        newUser.put("preferredLeagues", body.get("preferredLeagues", List.class));
        
        // Profile image as base64 (optional)
        if (body.containsKey("profileImage") && body.get("profileImage") != null) {
            String profileImageBase64 = body.getString("profileImage");
            // Validate base64 format (optional - starts with data:image/ or is pure base64)
            if (profileImageBase64 != null && !profileImageBase64.trim().isEmpty()) {
                newUser.put("profileImage", profileImageBase64);
                System.out.println("✅ Profile image added to user (length: " + profileImageBase64.length() + " chars)");
            }
        }
        
        authRepository.saveUser(newUser);
        convertIdToString(newUser);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    public boolean userExistenceByEmail(String email) {
        Document user = authRepository.findUserByEmail(email);
        return user != null;
    }

    public ResponseEntity<Document> sendResetCode(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ResponseEntity<>(new Document("error", "Email is required"), HttpStatus.BAD_REQUEST);
        }
        
        if (!userExistenceByEmail(email)) {
            return new ResponseEntity<>(new Document("error", "User not found"), HttpStatus.NOT_FOUND);
        }
        String code = RandomStringUtils.secure().nextNumeric(6);
        
        authRepository.saveResetCode(email, code);
        
        try {
            gmailMailService.sendEmail(email, code);
        } catch (RuntimeException e) {
            try {
                mailService.sendEmail(email, code);
            } catch (RuntimeException e2) {
                return new ResponseEntity<>(
                    new Document("error", "Failed to send email: " + e2.getMessage()), 
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        }
        
        return new ResponseEntity<>(
            new Document("success", true), 
            HttpStatus.OK
        );
    }
    
    public ResponseEntity<Document> resetPassword(String email, String code, String newPassword, String confirmPassword) {
        if (email == null || code == null || newPassword == null || confirmPassword == null) {
            return new ResponseEntity<>(
                new Document("error", "All fields are required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (!newPassword.equals(confirmPassword)) {
            return new ResponseEntity<>(
                new Document("error", "Passwords do not match"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (newPassword.length() < 6) {
            return new ResponseEntity<>(
                new Document("error", "Password must be at least 6 characters"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (!userExistenceByEmail(email)) {
            return new ResponseEntity<>(
                new Document("error", "User not found"), 
                HttpStatus.NOT_FOUND
            );
        }
        
        Document resetDoc = authRepository.findResetCode(email, code);
        
        if (resetDoc == null) {
            return new ResponseEntity<>(
                new Document("error", "Invalid or expired reset code"), 
                HttpStatus.UNAUTHORIZED
            );
        }
        
        long createdAt = resetDoc.getLong("createdAt");
        long thirtyMinutesInMillis = 30 * 60 * 1000;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - createdAt > thirtyMinutesInMillis) {
            authRepository.deleteResetCode(email);
            return new ResponseEntity<>(
                new Document("error", "Reset code has expired. Please request a new one."), 
                HttpStatus.UNAUTHORIZED
            );
        }
        
        Document user = authRepository.findUserByEmail(email);
        if (user != null) {
            @SuppressWarnings("unchecked")
            List<String> passwordHistory = (List<String>) user.get("passwords");            
            if (passwordHistory != null && !passwordHistory.isEmpty()) {
                System.out.println("🔍 DEBUG - Password History Size: " + passwordHistory.size());
                
                if (passwordHistory.contains(newPassword)) {
                    System.out.println("❌ DEBUG - Password already exists in history!");
                    return new ResponseEntity<>(
                        new Document("error", "You cannot reuse a previous password. Please choose a different password."), 
                        HttpStatus.BAD_REQUEST
                    );
                }
            }
        }
        
        authRepository.updateUserPassword(email, newPassword);
        
        authRepository.deleteResetCode(email);
        
        return new ResponseEntity<>(
            new Document("success", true), 
            HttpStatus.OK
        );
    }

    public ResponseEntity<Document> patchUserInfo(Document body, String id) {
        if (id == null || id.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "User ID is required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (!id.matches("^[0-9a-fA-F]{24}$")) {
            return new ResponseEntity<>(
                new Document("error", "Invalid user ID format"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        Document user = authRepository.findUserById(id);
        if (user == null) {
            return new ResponseEntity<>(
                new Document("error", "User not found"), 
                HttpStatus.NOT_FOUND
            );
        }
        
        if (body == null || body.isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "No fields to update"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        List<String> protectedFields = List.of("_id", "passwords");
        
        Document filteredUpdates = new Document();
        for (String key : body.keySet()) {
            if (!protectedFields.contains(key)) {
                Object value = body.get(key);
                
                // Special handling for profileImage (base64)
                if ("profileImage".equals(key) && value instanceof String) {
                    String profileImageBase64 = (String) value;
                    if (profileImageBase64 != null && !profileImageBase64.trim().isEmpty()) {
                        filteredUpdates.put(key, profileImageBase64);
                        System.out.println("✅ Profile image update requested (length: " + profileImageBase64.length() + " chars)");
                    } else {
                        // Allow setting to null/empty to remove image
                        filteredUpdates.put(key, null);
                        System.out.println("✅ Profile image removal requested");
                    }
                } else {
                    filteredUpdates.put(key, value);
                }
            }
        }
        
        if (filteredUpdates.isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "No valid fields to update"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        updateDocumentFields(user, filteredUpdates);
        authRepository.saveUser(user);
        convertIdToString(user);
        
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
    
    private void updateDocumentFields(Document original, Document updates) {
        for (String key : updates.keySet()) {
            Object value = updates.get(key);
    
            if (original.containsKey(key)) {
                if (value instanceof Document && original.get(key) instanceof Document) {
                    updateDocumentFields((Document) original.get(key), (Document) value);
                } else {
                    original.put(key, value);
                }
            } else {
                original.put(key, value);
            }
        }
    }

    public ResponseEntity<Document> deleteUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "User ID is required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (!userId.matches("^[0-9a-fA-F]{24}$")) {
            return new ResponseEntity<>(
                new Document("error", "Invalid user ID format"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        Document user = authRepository.findUserById(userId);
        if (user == null) {
            return new ResponseEntity<>(
                new Document("error", "User not found"), 
                HttpStatus.NOT_FOUND
            );
        }
        
        try {
            authRepository.deleteUser(userId);
            return new ResponseEntity<>(
                new Document("message", "User deleted successfully"), 
                HttpStatus.OK
            );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                new Document("error", "Invalid user ID format"), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private void convertIdToString(Document document) {
        if (document.containsKey("_id")) {
            Object id = document.get("_id");
            if (id instanceof org.bson.types.ObjectId) {
                document.put("_id", ((org.bson.types.ObjectId) id).toString());
            }
        }
    }
    
    /**
     * Handles social authentication (Facebook OAuth)
     * Creates new user if doesn't exist, or returns existing user
     * 
     * @param accessToken The Facebook access token
     * @param provider The auth provider (e.g., "facebook")
     * @return ResponseEntity with user data, tokens, and profileIncomplete flag
     */
    public ResponseEntity<Document> authenticateWithSocial(String accessToken, String provider) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "Access token is required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (!"facebook".equalsIgnoreCase(provider)) {
            return new ResponseEntity<>(
                new Document("error", "Only Facebook provider is currently supported"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        try {
            // Validate token with Facebook and get user info
            java.util.Map<String, Object> facebookData = facebookAuthService.validateTokenAndGetUserInfo(accessToken);
            
            String facebookId = (String) facebookData.get("id");
            String email = (String) facebookData.get("email");
            String fullName = (String) facebookData.get("name");
            String profilePicture = facebookAuthService.extractProfilePicture(facebookData);
            
            if (email == null || email.trim().isEmpty()) {
                return new ResponseEntity<>(
                    new Document("error", "Facebook account does not have an email associated"), 
                    HttpStatus.BAD_REQUEST
                );
            }
            
            // Split name into firstName and lastName
            String[] nameParts = facebookAuthService.splitName(fullName);
            String firstName = nameParts[0];
            String lastName = nameParts[1];
            
            // Check if user already exists (by email or facebookId)
            Document existingUser = authRepository.findUserByEmail(email);
            
            if (existingUser != null) {
                // Update Facebook info if not set
                if (existingUser.get("facebookId") == null) {
                    existingUser.put("facebookId", facebookId);
                }
                if (existingUser.get("authProvider") == null) {
                    existingUser.put("authProvider", "facebook");
                }
                if (existingUser.get("profilePicture") == null && profilePicture != null) {
                    existingUser.put("profilePicture", profilePicture);
                }
                
                authRepository.saveUser(existingUser);
                return generateSocialAuthResponse(existingUser, email);
            }
            
            // Create new user
            Document newUser = new Document();
            newUser.put("email", email);
            newUser.put("name", firstName);
            newUser.put("lastName", lastName);
            newUser.put("facebookId", facebookId);
            newUser.put("authProvider", "facebook");
            newUser.put("profilePicture", profilePicture);
            newUser.put("passwords", new ArrayList<>()); // Empty password list for OAuth users
            newUser.put("preferredTeams", new ArrayList<>());
            newUser.put("preferredLeagues", new ArrayList<>());
            newUser.put("profileIncomplete", true);
            
            authRepository.saveUser(newUser);
            
            return generateSocialAuthResponse(newUser, email);
            
        } catch (RuntimeException e) {
            return new ResponseEntity<>(
                new Document("error", "Facebook authentication failed: " + e.getMessage()), 
                HttpStatus.UNAUTHORIZED
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Document("error", "Internal server error: " + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Completes user profile with required fields (preferredTeams, preferredLeagues)
     * 
     * @param body Document containing the fields to update
     * @param userId The user ID from JWT token
     * @return ResponseEntity with updated user data
     */
    public ResponseEntity<Document> completeProfile(Document body, String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "User ID is required"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (!userId.matches("^[0-9a-fA-F]{24}$")) {
            return new ResponseEntity<>(
                new Document("error", "Invalid user ID format"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        Document user = authRepository.findUserById(userId);
        if (user == null) {
            return new ResponseEntity<>(
                new Document("error", "User not found"), 
                HttpStatus.NOT_FOUND
            );
        }
        
        // Update allowed fields
        List<String> allowedFields = List.of(
            "preferredTeams", "preferredLeagues", "birth", "country", 
            "state", "city", "phone", "zipcode", "profileImage", "name", "lastName"
        );
        
        Document updates = new Document();
        for (String field : allowedFields) {
            if (body.containsKey(field)) {
                Object value = body.get(field);
                
                // Special handling for profileImage (base64)
                if ("profileImage".equals(field) && value instanceof String) {
                    String profileImageBase64 = (String) value;
                    if (profileImageBase64 != null && !profileImageBase64.trim().isEmpty()) {
                        updates.put(field, profileImageBase64);
                        System.out.println("✅ Profile image update in completeProfile (length: " + profileImageBase64.length() + " chars)");
                    } else {
                        updates.put(field, null); // Allow removing image
                    }
                } else {
                    updates.put(field, value);
                }
            }
        }
        
        if (updates.isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "No valid fields to update"), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        updateDocumentFields(user, updates);
        
        // Check if profile is now complete
        @SuppressWarnings("unchecked")
        List<String> preferredTeams = (List<String>) user.get("preferredTeams");
        @SuppressWarnings("unchecked")
        List<String> preferredLeagues = (List<String>) user.get("preferredLeagues");
        
        boolean isComplete = preferredTeams != null && !preferredTeams.isEmpty() 
                          && preferredLeagues != null && !preferredLeagues.isEmpty();
        
        user.put("profileIncomplete", !isComplete);
        
        authRepository.saveUser(user);
        
        // Generate new response with tokens
        String email = user.getString("email");
        return generateSocialAuthResponse(user, email);
    }
    
    /**
     * Helper method to generate response for social auth with tokens
     */
    private ResponseEntity<Document> generateSocialAuthResponse(Document user, String email) {
        convertIdToString(user);
        
        String userId = user.getString("_id");
        String accessToken = jwtService.generateToken(email, userId);
        String refreshToken = jwtService.generateRefreshToken(email, userId);
        
        // Check if profile is complete
        @SuppressWarnings("unchecked")
        List<String> preferredTeams = (List<String>) user.get("preferredTeams");
        @SuppressWarnings("unchecked")
        List<String> preferredLeagues = (List<String>) user.get("preferredLeagues");
        
        boolean isComplete = preferredTeams != null && !preferredTeams.isEmpty() 
                          && preferredLeagues != null && !preferredLeagues.isEmpty();
        
        Document response = new Document();
        response.putAll(user);
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400);
        response.put("profileIncomplete", !isComplete);
        
        // Add missing fields list if profile is incomplete
        if (!isComplete) {
            List<String> missingFields = new ArrayList<>();
            if (preferredTeams == null || preferredTeams.isEmpty()) {
                missingFields.add("preferredTeams");
            }
            if (preferredLeagues == null || preferredLeagues.isEmpty()) {
                missingFields.add("preferredLeagues");
            }
            response.put("missingFields", missingFields);
        }
        
        // Remove sensitive data
        response.remove("passwords");
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Check if email exists in the system
     */
    public ResponseEntity<Document> checkEmailExists(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                Document error = new Document("error", "Email is required");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            Document user = authRepository.findUserByEmail(email);
            Document response = new Document();
            
            if (user != null) {
                convertIdToString(user);
                response.put("exists", true);
                response.put("userId", user.getString("_id"));
                response.put("name", user.getString("name"));
                response.put("lastName", user.getString("lastName"));
                response.put("email", user.getString("email"));
                response.put("profileImage", user.getString("profileImage"));
            } else {
                response.put("exists", false);
                response.put("email", email);
            }
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("❌ Error checking email: " + e.getMessage());
            Document error = new Document("error", "Error checking email: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Save or update a match prediction for a user
     * New structure: predictions organized by competitionId with matchInfo array and accumulated points
     * The FE sends realTeam1Score, realTeam2Score, etc. for point calculation
     */
    public ResponseEntity<Document> savePrediction(String userId, List<String> groupIds, String matchId, 
                                                   Integer team1Score, Integer team2Score,
                                                   Integer realTeam1Score, Integer realTeam2Score,
                                                   Boolean extraTime, Boolean realExtraTime,
                                                   Integer penaltiesteam1Score, Integer penaltiesteam2Score,
                                                   String competitionId, String team1, String team2) {
        // Validate required fields
        if (userId == null || matchId == null || team1Score == null || team2Score == null || 
            realTeam1Score == null || realTeam2Score == null || 
            groupIds == null || groupIds.isEmpty() || competitionId == null || competitionId.trim().isEmpty() ||
            team1 == null || team1.trim().isEmpty() || team2 == null || team2.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "All fields are required: userId, matchId, team1Score, team2Score, realTeam1Score, realTeam2Score, groupIds, competitionId, team1, team2"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        try {
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("✅ Saving prediction - START");
            System.out.println("   userId: " + userId);
            System.out.println("   matchId: " + matchId);
            System.out.println("   competitionId: " + competitionId);
            System.out.println("   groupIds: " + groupIds);
            System.out.println("   groupIds count: " + (groupIds != null ? groupIds.size() : 0));
            System.out.println("   team1Score: " + team1Score + ", team2Score: " + team2Score);
            System.out.println("   realTeam1Score: " + realTeam1Score + ", realTeam2Score: " + realTeam2Score);
            System.out.println("═══════════════════════════════════════════════════════");
            
            // 1. Save prediction with new structure (by competitionId)
            try {
                authRepository.savePrediction(userId, competitionId, matchId, 
                                             team1Score, team2Score,
                                             realTeam1Score, realTeam2Score,
                                             extraTime, realExtraTime,
                                             penaltiesteam1Score, penaltiesteam2Score,
                                             team1, team2);
                System.out.println("✅ Step 1: Prediction saved to database");
            } catch (Exception e) {
                System.err.println("❌ ERROR saving prediction to database: " + e.getMessage());
                e.printStackTrace();
                throw e; // Re-throw to be caught by outer catch
            }
            
            // 2. Get the competition points (accumulated from all matches)
            Integer competitionPoints = null;
            try {
                competitionPoints = authRepository.getCompetitionPoints(userId, competitionId);
                if (competitionPoints == null) {
                    competitionPoints = 0; // Default to 0 if null
                }
                System.out.println("✅ Step 2: Competition points retrieved: " + competitionPoints);
            } catch (Exception e) {
                System.err.println("❌ ERROR getting competition points: " + e.getMessage());
                e.printStackTrace();
                competitionPoints = 0; // Default to 0 on error
            }
            
            // 3. Get the matchInfo from the saved prediction to send to groups
            Document user = null;
            try {
                user = authRepository.findUserById(userId);
                if (user == null) {
                    System.err.println("❌ User not found after saving prediction: " + userId);
                    return new ResponseEntity<>(
                        new Document("error", "User not found after saving prediction"),
                        HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
                System.out.println("✅ Step 3: User document retrieved");
            } catch (Exception e) {
                System.err.println("❌ ERROR finding user: " + e.getMessage());
                e.printStackTrace();
                return new ResponseEntity<>(
                    new Document("error", "Error retrieving user after saving prediction: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            
            // Get predictions document
            @SuppressWarnings("unchecked")
            Document predictions = null;
            try {
                Object predictionsObj = user.get("predictions");
                if (predictionsObj instanceof Document) {
                    predictions = (Document) predictionsObj;
                } else {
                    System.err.println("❌ Predictions is not a Document, type: " + (predictionsObj != null ? predictionsObj.getClass().getName() : "null"));
                    return new ResponseEntity<>(
                        new Document("error", "Predictions structure is invalid"),
                        HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
                
                if (predictions == null) {
                    System.err.println("❌ Predictions not found after saving");
                    return new ResponseEntity<>(
                        new Document("error", "Predictions not found after saving"),
                        HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
                System.out.println("✅ Step 4: Predictions document retrieved");
            } catch (Exception e) {
                System.err.println("❌ ERROR getting predictions: " + e.getMessage());
                e.printStackTrace();
                return new ResponseEntity<>(
                    new Document("error", "Error getting predictions: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            
            // Get competition prediction
            @SuppressWarnings("unchecked")
            Document competitionPrediction = null;
            try {
                competitionPrediction = (Document) predictions.get(competitionId);
                if (competitionPrediction == null) {
                    System.err.println("❌ Competition prediction not found for: " + competitionId);
                    return new ResponseEntity<>(
                        new Document("error", "Competition prediction not found after saving"),
                        HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
                System.out.println("✅ Step 5: Competition prediction retrieved");
            } catch (Exception e) {
                System.err.println("❌ ERROR getting competition prediction: " + e.getMessage());
                e.printStackTrace();
                return new ResponseEntity<>(
                    new Document("error", "Error getting competition prediction: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            
            // Get matchInfo
            @SuppressWarnings("unchecked")
            List<Document> matchInfo = null;
            try {
                Object matchInfoObj = competitionPrediction.get("matchInfo");
                if (matchInfoObj instanceof List) {
                    matchInfo = (List<Document>) matchInfoObj;
                } else {
                    System.err.println("⚠️ matchInfo is not a List, creating empty list");
                    matchInfo = new ArrayList<>();
                }
                
                if (matchInfo == null) {
                    matchInfo = new ArrayList<>();
                }
                System.out.println("✅ Step 6: matchInfo retrieved, size: " + matchInfo.size());
            } catch (Exception e) {
                System.err.println("❌ ERROR getting matchInfo: " + e.getMessage());
                e.printStackTrace();
                matchInfo = new ArrayList<>(); // Default to empty list
            }
            
            // Convert matchInfo (List<Document>) to List<Map<String, Object>> for the API call
            // This will be copied exactly to matchesInfo in each group's users[] array
            List<Map<String, Object>> matchesDetail = new ArrayList<>();
            if (matchInfo != null && !matchInfo.isEmpty()) {
                System.out.println("📋 Converting " + matchInfo.size() + " matches from matchInfo to matchesDetail");
                for (Document matchDoc : matchInfo) {
                    if (matchDoc == null) {
                        System.err.println("⚠️ Warning: null matchDoc found in matchInfo, skipping");
                        continue;
                    }
                    
                    try {
                        Map<String, Object> matchMap = new java.util.HashMap<>();
                        
                        // Copy all fields from matchInfo exactly as they are
                        matchMap.put("matchId", matchDoc.getString("matchId"));
                        matchMap.put("team1", matchDoc.getString("team1"));
                        matchMap.put("team2", matchDoc.getString("team2"));
                        matchMap.put("team1Score", matchDoc.getInteger("team1Score", 0));
                        matchMap.put("team2Score", matchDoc.getInteger("team2Score", 0));
                        matchMap.put("realTeam1Score", matchDoc.getInteger("realTeam1Score", 0));
                        matchMap.put("realTeam2Score", matchDoc.getInteger("realTeam2Score", 0));
                        
                        // Copy predictedDate (important field)
                        if (matchDoc.containsKey("predictedDate")) {
                            matchMap.put("predictedDate", matchDoc.get("predictedDate"));
                        }
                        
                        // Copy optional fields for knockout stages
                        if (matchDoc.containsKey("extraTime")) {
                            matchMap.put("extraTime", matchDoc.getBoolean("extraTime"));
                        }
                        if (matchDoc.containsKey("realExtraTime")) {
                            matchMap.put("realExtraTime", matchDoc.getBoolean("realExtraTime"));
                        }
                        if (matchDoc.containsKey("penaltiesteam1Score")) {
                            matchMap.put("penaltiesteam1Score", matchDoc.getInteger("penaltiesteam1Score"));
                        }
                        if (matchDoc.containsKey("penaltiesteam2Score")) {
                            matchMap.put("penaltiesteam2Score", matchDoc.getInteger("penaltiesteam2Score"));
                        }
                        
                        matchesDetail.add(matchMap);
                    } catch (Exception e) {
                        System.err.println("⚠️ Error converting matchDoc to Map: " + e.getMessage());
                        e.printStackTrace();
                        // Continue with next match
                    }
                }
                System.out.println("✅ Converted " + matchesDetail.size() + " matches from matchInfo to matchesDetail");
                if (!matchesDetail.isEmpty()) {
                    System.out.println("   First match: " + matchesDetail.get(0));
                }
            } else {
                System.out.println("⚠️ matchInfo is null or empty, matchesDetail will be empty");
            }
            
            // 4. Update all groups at once with matchesDetail and user score
            // This will update each group's users[] array with:
            // - users[].score: user's accumulated score
            // - users[].matchesInfo: array with matchInfo (copied from predictions[competitionId].matchInfo)
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("📤 Updating " + groupIds.size() + " groups with matchesDetail and user score");
            System.out.println("   groupIds: " + groupIds);
            System.out.println("   userId: " + userId);
            System.out.println("   competitionId: " + competitionId);
            System.out.println("   matchesDetail size: " + (matchesDetail != null ? matchesDetail.size() : 0));
            System.out.println("   competitionPoints: " + competitionPoints);
            System.out.println("═══════════════════════════════════════════════════════");
            
            boolean updated = false;
            try {
                updated = groupsClientService.updateMultipleGroupsWithMatchesDetail(
                    groupIds, 
                    userId, 
                    competitionId, 
                    matchesDetail, 
                    competitionPoints
                );
                System.out.println("✅ Call to updateMultipleGroupsWithMatchesDetail completed. Result: " + updated);
            } catch (Exception e) {
                System.err.println("❌ ERROR calling updateMultipleGroupsWithMatchesDetail: " + e.getMessage());
                e.printStackTrace();
                // Continue anyway, don't fail the whole request
            }
            
            // Build groups applied list for response
            List<Document> groupsApplied = new ArrayList<>();
            for (String targetGroupId : groupIds) {
                // Get group info to get group name (with error handling)
                String groupName = "Unknown";
                try {
                    Map<String, Object> groupInfo = groupsClientService.getGroupInfoInternal(targetGroupId);
                    if (groupInfo != null) {
                        String name = (String) groupInfo.get("name");
                        if (name != null && !name.trim().isEmpty()) {
                            groupName = name;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Could not get group info for " + targetGroupId + ": " + e.getMessage());
                    // Continue with "Unknown" as group name
                }
                
                Document groupResult = new Document();
                groupResult.put("groupId", targetGroupId);
                groupResult.put("groupName", groupName);
                groupResult.put("pointsCalculated", updated);
                groupResult.put("points", competitionPoints); // Total points for the competition
                groupResult.put("totalScore", competitionPoints); // Same as points (accumulated)
                groupsApplied.add(groupResult);
            }
            
            if (!updated) {
                System.err.println("⚠️ Failed to update some groups with matchesDetail");
            } else {
                System.out.println("✅ Successfully updated all " + groupIds.size() + " groups");
            }
            
            // 4. Build response
            Document response = new Document();
            response.put("message", "Prediction saved successfully");
            response.put("userId", userId);
            response.put("matchId", matchId);
            response.put("competitionId", competitionId);
            response.put("team1Score", team1Score);
            response.put("team2Score", team2Score);
            response.put("realTeam1Score", realTeam1Score);
            response.put("realTeam2Score", realTeam2Score);
            response.put("points", competitionPoints);
            response.put("groupsApplied", groupsApplied);
            
            if (extraTime != null) {
                response.put("extraTime", extraTime);
            }
            if (realExtraTime != null) {
                response.put("realExtraTime", realExtraTime);
            }
            if (penaltiesteam1Score != null) {
                response.put("penaltiesteam1Score", penaltiesteam1Score);
            }
            if (penaltiesteam2Score != null) {
                response.put("penaltiesteam2Score", penaltiesteam2Score);
            }
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("═══════════════════════════════════════════════════════");
            System.err.println("❌ ERROR saving prediction:");
            System.err.println("   userId: " + userId);
            System.err.println("   matchId: " + matchId);
            System.err.println("   competitionId: " + competitionId);
            System.err.println("   groupIds: " + groupIds);
            System.err.println("   Error message: " + e.getMessage());
            System.err.println("   Error class: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("═══════════════════════════════════════════════════════");
            
            Document errorResponse = new Document();
            errorResponse.put("error", "Error saving prediction: " + e.getMessage());
            errorResponse.put("errorType", e.getClass().getName());
            if (e.getCause() != null) {
                errorResponse.put("cause", e.getCause().getMessage());
            }
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    /**
     * Calculate points for a prediction based on actual match results
     */
    private Integer calculatePredictionPoints(Integer predTeam1Score, Integer predTeam2Score,
                                            Integer actualTeam1Score, Integer actualTeam2Score,
                                            Boolean userExtraTime, Boolean userPenalties,
                                            Integer userPenaltiesTeam1Score, Integer userPenaltiesTeam2Score,
                                            Map<String, Object> matchData) {
        int points = 0;
        
        // Normalize prediction scores (null = 0)
        if (predTeam1Score == null) predTeam1Score = 0;
        if (predTeam2Score == null) predTeam2Score = 0;
        
        // Normalize actual scores (null = 0)
        if (actualTeam1Score == null) actualTeam1Score = 0;
        if (actualTeam2Score == null) actualTeam2Score = 0;
        
        // Check for exact score match
        if (predTeam1Score.equals(actualTeam1Score) && predTeam2Score.equals(actualTeam2Score)) {
            points = 5; // Exact score: 5 points
        } else {
            // Check for correct result (win/draw/loss)
            String predictedResult;
            if (predTeam1Score > predTeam2Score) {
                predictedResult = "team1_win";
            } else if (predTeam2Score > predTeam1Score) {
                predictedResult = "team2_win";
            } else {
                predictedResult = "draw";
            }
            
            String actualResult;
            if (actualTeam1Score > actualTeam2Score) {
                actualResult = "team1_win";
            } else if (actualTeam2Score > actualTeam1Score) {
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
        
        // Additional points for knockout matches (extra time and penalties)
        String stageId = (String) matchData.get("stageId");
        boolean isKnockout = stageId != null && !stageId.equals("group-stage");
        
        if (isKnockout) {
            // Check extra time prediction
            Boolean actualExtraTime = (Boolean) matchData.get("extraTime");
            if (userExtraTime != null && userExtraTime && actualExtraTime != null && actualExtraTime) {
                points += 1; // +1 point for correct extra time prediction
            }
            
            // Check penalties prediction
            Boolean actualPenalties = (Boolean) matchData.get("penalties");
            if (userPenalties != null && userPenalties && actualPenalties != null && actualPenalties) {
                points += 2; // +2 points for correct penalties prediction
                
                // Check exact penalties score
                Object penaltiesTeam1ScoreObj = matchData.get("penaltiesTeam1Score");
                Object penaltiesTeam2ScoreObj = matchData.get("penaltiesTeam2Score");
                
                Integer actualPenaltiesTeam1Score = null;
                Integer actualPenaltiesTeam2Score = null;
                
                if (penaltiesTeam1ScoreObj != null) {
                    actualPenaltiesTeam1Score = penaltiesTeam1ScoreObj instanceof Integer ? (Integer) penaltiesTeam1ScoreObj : 
                                               (penaltiesTeam1ScoreObj instanceof Number ? ((Number) penaltiesTeam1ScoreObj).intValue() : null);
                }
                
                if (penaltiesTeam2ScoreObj != null) {
                    actualPenaltiesTeam2Score = penaltiesTeam2ScoreObj instanceof Integer ? (Integer) penaltiesTeam2ScoreObj : 
                                               (penaltiesTeam2ScoreObj instanceof Number ? ((Number) penaltiesTeam2ScoreObj).intValue() : null);
                }
                
                if (userPenaltiesTeam1Score != null && userPenaltiesTeam2Score != null &&
                    actualPenaltiesTeam1Score != null && actualPenaltiesTeam2Score != null &&
                    userPenaltiesTeam1Score.equals(actualPenaltiesTeam1Score) &&
                    userPenaltiesTeam2Score.equals(actualPenaltiesTeam2Score)) {
                    points += 3; // +3 points for exact penalties score
                }
            }
        }
        
        return points;
    }
    
    /**
     * Get all predictions for a user, optionally filtered by groupId
     * Returns predictions organized by competitionId
     */
    public ResponseEntity<Document> getUserPredictions(String userId, String groupId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "User ID is required"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        try {
            Document predictions = authRepository.getUserPredictions(userId, groupId);
            Document response = new Document();
            response.put("userId", userId);
            response.put("groupId", groupId != null ? groupId : "all");
            response.put("predictions", predictions);
            // Count competitions
            int competitionCount = predictions != null ? predictions.size() : 0;
            response.put("count", competitionCount);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("❌ Error getting predictions: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(
                new Document("error", "Error getting predictions: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Get a specific prediction for a user, group, and match (internal endpoint with service token)
     */
    public ResponseEntity<Document> getUserPredictionInternal(String userId, String groupId, String matchId, String serviceToken) {
        // Validate service token
        if (serviceToken == null || serviceToken.trim().isEmpty()) {
            return new ResponseEntity<>(
                new Document("error", "Service token is required"), 
                HttpStatus.UNAUTHORIZED
            );
        }
        
        if (!serviceToken.equals(this.serviceToken)) {
            return new ResponseEntity<>(
                new Document("error", "Invalid service token"), 
                HttpStatus.FORBIDDEN
            );
        }
        
        // Get prediction
        return getUserPrediction(userId, groupId, matchId);
    }
    
    /**
     * Get a specific prediction for a user, group, and match
     */
    public ResponseEntity<Document> getUserPrediction(String userId, String groupId, String matchId) {
        if (userId == null || groupId == null || matchId == null) {
            return new ResponseEntity<>(
                new Document("error", "userId, groupId, and matchId are required"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        try {
            Document prediction = authRepository.getUserPrediction(userId, groupId, matchId);
            Document response = new Document();
            if (prediction != null) {
                response.put("prediction", prediction);
                response.put("exists", true);
            } else {
                response.put("prediction", null);
                response.put("exists", false);
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("❌ Error getting prediction: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(
                new Document("error", "Error getting prediction: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Update prediction points after calculating scores
     * Points are now automatically recalculated when match info is updated
     */
    public ResponseEntity<Document> updatePredictionPoints(String userId, String competitionId, String matchId) {
        if (userId == null || competitionId == null || matchId == null) {
            return new ResponseEntity<>(
                new Document("error", "All fields are required: userId, competitionId, matchId"),
                HttpStatus.BAD_REQUEST
            );
        }
        
        try {
            authRepository.updatePredictionPoints(userId, competitionId, matchId);
            Integer points = authRepository.getCompetitionPoints(userId, competitionId);
            Document response = new Document();
            response.put("message", "Prediction points updated successfully");
            response.put("userId", userId);
            response.put("competitionId", competitionId);
            response.put("matchId", matchId);
            response.put("points", points);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("❌ Error updating prediction points: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(
                new Document("error", "Error updating prediction points: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}