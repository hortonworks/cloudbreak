package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@EntityType(entityClass = DatabaseServerConfig.class)
@Transactional(TxType.REQUIRED)
public interface DatabaseServerConfigRepository extends JpaRepository<DatabaseServerConfig, Long> {

    Set<DatabaseServerConfig> findByWorkspaceIdAndEnvironmentId(Long workspaceId, String environmentId);

    Optional<DatabaseServerConfig> findByResourceCrn(Crn crn);

    Optional<DatabaseServerConfig> findByNameAndWorkspaceIdAndEnvironmentId(String name, Long workspaceId, String environmentId);

    Set<DatabaseServerConfig> findByResourceCrnIn(Set<Crn> resourceCrns);

    @Query("SELECT s FROM DatabaseServerConfig s WHERE s.workspaceId = :workspaceId AND s.environmentId = :environmentId "
            + "AND (s.name IN :names OR s.resourceCrn IN :names)")
    Set<DatabaseServerConfig> findByNameInAndWorkspaceIdAndEnvironmentId(
            @Param("names") Set<String> names,
            @Param("workspaceId") Long workspaceId,
            @Param("environmentId") String environmentId);
}
