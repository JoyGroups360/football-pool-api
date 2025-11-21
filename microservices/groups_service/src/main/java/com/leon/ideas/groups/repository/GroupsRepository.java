package com.leon.ideas.groups.repository;

import com.leon.ideas.groups.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupsRepository extends MongoRepository<Group, String> {
    
    // Find groups by creator
    List<Group> findByCreatorUserId(String creatorUserId);
    
    // Find groups where user is a member (search in users.id)
    @Query("{ 'users.id': ?0 }")
    List<Group> findByUsersId(String userId);
    
    // Find groups by competition
    List<Group> findByCompetitionId(String competitionId);
    
    // Find groups where user is creator OR member (search in users.id)
    @Query("{ $or: [ { 'creatorUserId': ?0 }, { 'users.id': ?0 } ] }")
    List<Group> findGroupsByUserIdAsCreatorOrMember(String userId);
    
    // Find groups by invited email (pending invitations)
    List<Group> findByInvitedEmailsContaining(String email);
    
    // Find group by creator and competition (to check if already exists)
    Group findByCreatorUserIdAndCompetitionId(String creatorUserId, String competitionId);
    
    // Find group by creator, competition AND name (to check if already exists with same name)
    Group findByCreatorUserIdAndCompetitionIdAndName(String creatorUserId, String competitionId, String name);
}


