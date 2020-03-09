package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.BaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;
import com.sequenceiq.cloudbreak.domain.UserProfile;

@EntityType(entityClass = UserProfile.class)
@Transactional(TxType.REQUIRED)
@HasPermission
public interface UserProfileRepository extends BaseRepository<UserProfile, Long> {
    @Query("SELECT b FROM UserProfile b "
            + "LEFT JOIN FETCH b.user u "
            + "LEFT JOIN FETCH u.tenant "
            + "WHERE u.id= :userId")
    Optional<UserProfile> findOneByUser(@Param("userId") Long userId);

    @Query("SELECT b FROM UserProfile b WHERE b.imageCatalog.id = :catalogId")
    Set<UserProfile> findOneByImageCatalogName(@Param("catalogId") Long catalogId);
}
