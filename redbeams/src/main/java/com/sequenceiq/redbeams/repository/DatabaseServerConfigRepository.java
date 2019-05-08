package com.sequenceiq.redbeams.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

// FIXME: Use DisabledBaseRepository when doing permissions
@EntityType(entityClass = DatabaseServerConfig.class)
@Transactional(TxType.REQUIRED)
public interface DatabaseServerConfigRepository extends CrudRepository<DatabaseServerConfig, Long> {

    @Query("SELECT s FROM DatabaseServerConfig s WHERE"
            + " s.workspaceId = :workspaceId AND s.environmentId = :environmentId"
            + " AND s.resourceStatus = 'USER_MANAGED'")
    Set<DatabaseServerConfig> findAllByWorkspaceIdAndEnvironmentId(Long workspaceId, String environmentId);

}
