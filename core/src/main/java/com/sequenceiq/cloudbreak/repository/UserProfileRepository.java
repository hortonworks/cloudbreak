package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = UserProfile.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface UserProfileRepository extends BaseRepository<UserProfile, Long> {

    @Query("SELECT b FROM UserProfile b LEFT JOIN FETCH b.user WHERE b.user.id= :userId")
    UserProfile findOneByUser(@Param("userId") Long userId);

    @Query("SELECT b FROM UserProfile b JOIN b.defaultCredentials c WHERE c.id = :credentialId")
    Set<UserProfile> findOneByCredentialId(@Param("credentialId") Long credentialId);

    @Query("SELECT b FROM UserProfile b WHERE b.imageCatalog.id = :catalogId")
    Set<UserProfile> findOneByImageCatalogName(@Param("catalogId") Long catalogId);
}
