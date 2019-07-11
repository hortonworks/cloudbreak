package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.authorization.CheckPermissionsByReturnValue;
import com.sequenceiq.redbeams.authorization.ResourceAction;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@EntityType(entityClass = DatabaseServerConfig.class)
@Transactional(TxType.REQUIRED)
public interface DatabaseServerConfigRepository extends JpaRepository<DatabaseServerConfig, Long> {

    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    Set<DatabaseServerConfig> findByWorkspaceIdAndEnvironmentId(Long workspaceId, String environmentId);

    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    @Query("SELECT s FROM DatabaseServerConfig s WHERE s.workspaceId = :workspaceId AND s.environmentId = :environmentId "
            + "AND (s.name = :name OR s.resourceCrn = :name)")
    Optional<DatabaseServerConfig> findByNameAndWorkspaceIdAndEnvironmentId(
            @Param("name") String name,
            @Param("workspaceId") Long workspaceId,
            @Param("environmentId") String environmentId);

    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    @Query("SELECT s FROM DatabaseServerConfig s WHERE s.workspaceId = :workspaceId AND s.environmentId = :environmentId "
            + "AND (s.name IN :names OR s.resourceCrn IN :names)")
    Set<DatabaseServerConfig> findByNameInAndWorkspaceIdAndEnvironmentId(
            @Param("names") Set<String> names,
            @Param("workspaceId") Long workspaceId,
            @Param("environmentId") String environmentId);

    // save does not require a permission check
}
