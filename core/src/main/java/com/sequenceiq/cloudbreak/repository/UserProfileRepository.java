package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.UserProfile;

@EntityType(entityClass = UserProfile.class)
public interface UserProfileRepository extends CrudRepository<UserProfile, Long> {

    @Query("SELECT b FROM UserProfile b WHERE b.owner= :owner and b.account= :account")
    UserProfile findOneByOwnerAndAccount(@Param("owner") String owner, @Param("account") String account);

    @Query("SELECT b FROM UserProfile b WHERE b.credential.id = :credentialId")
    Set<UserProfile> findOneByCredentialId(@Param("credentialId") Long credentialId);

}
