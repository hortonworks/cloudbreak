package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

// FIXME: Use DisabledBaseRepository when doing permissions
@EntityType(entityClass = DatabaseServerConfig.class)
@Transactional(TxType.REQUIRED)
public interface DatabaseServerConfigRepository extends JpaRepository<DatabaseServerConfig, Long> {

    Set<DatabaseServerConfig> findAllByWorkspaceIdAndEnvironmentId(Long workspaceId, String environmentId);

    Optional<DatabaseServerConfig> findByNameAndWorkspaceIdAndEnvironmentId(String name, Long workspaceId, String environmentId);

    Set<DatabaseServerConfig> findByNameInAndWorkspaceIdAndEnvironmentId(Set<String> names, Long workspaceId, String environmentId);

}
