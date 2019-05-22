package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

// FIXME: Use DisabledBaseRepository when doing permissions
@EntityType(entityClass = DatabaseServerConfig.class)
@Transactional(TxType.REQUIRED)
public interface DatabaseServerConfigRepository extends JpaRepository<DatabaseServerConfig, Long> {

    @Query("SELECT s FROM DatabaseServerConfig s WHERE"
            + " s.workspaceId = :workspaceId AND s.environmentId = :environmentId"
            + " AND s.resourceStatus = 'USER_MANAGED'")
    Set<DatabaseServerConfig> findAllByWorkspaceIdAndEnvironmentId(@Param("workspaceId")Long workspaceId, @Param("environmentId")String environmentId);

    Optional<DatabaseServerConfig> findByNameAndWorkspaceIdAndEnvironmentId(String name, Long workspaceId, String environmentId);

    Optional<DatabaseServerConfig> findByNameAndEnvironmentId(String name, String  workspaceId);

    Set<DatabaseServerConfig> findByNameInAndWorkspaceIdAndEnvironmentId(Set<String> names, Long workspaceId, String environmentId);

}
