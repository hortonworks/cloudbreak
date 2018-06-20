package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.BaseRepository;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = UserProfile.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface UserProfileRepository extends BaseRepository<UserProfile, Long> {

    @Query("SELECT b FROM UserProfile b WHERE b.owner= :owner and b.account= :account")
    UserProfile findOneByOwnerAndAccount(@Param("account") String account, @Param("owner") String owner);

    @Query("SELECT b FROM UserProfile b WHERE b.credential.id = :credentialId")
    Set<UserProfile> findOneByCredentialId(@Param("credentialId") Long credentialId);

}
