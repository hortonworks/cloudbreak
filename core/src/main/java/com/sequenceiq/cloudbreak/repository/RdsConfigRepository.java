package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = RDSConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.DATABASE)
public interface RdsConfigRepository extends EnvironmentResourceRepository<RDSConfig, Long> {

    @Override
    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.workspace.id = :workspaceId AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Override
    @CheckPermissionsByWorkspace(action = READ, workspaceIndex = 0)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.workspace = :org AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByWorkspace(@Param("org") Workspace org);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name AND r.status = 'USER_MANAGED'")
    RDSConfig findUserManagedByName(@Param("name") String name);

    @Override
    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name AND r.status = 'USER_MANAGED'"
            + "AND r.workspace.id = :workspaceId")
    Optional<RDSConfig> findByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @Override
    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name in :names AND r.status = 'USER_MANAGED'"
            + "AND r.workspace.id = :workspaceId")
    Set<RDSConfig> findByNameInAndWorkspaceId(@Param("names") Set<String> name, @Param("workspaceId") Long workspaceId);

    @Override
    @CheckPermissionsByWorkspace(action = READ, workspaceIndex = 1)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name AND r.status = 'USER_MANAGED'"
            + "AND r.workspace = :org")
    Optional<RDSConfig> findByNameAndWorkspace(@Param("name") String name, @Param("org") Workspace org);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.id= :id AND r.status <> 'DEFAULT_DELETED'")
    Optional<RDSConfig> findById(@Param("id") Long id);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster LEFT JOIN FETCH r.clusters WHERE cluster.id= :clusterId")
    Set<RDSConfig> findByClusterId(@Param("clusterId") Long clusterId);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster LEFT JOIN FETCH r.clusters WHERE cluster.id= :clusterId "
            + "AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findUserManagedByClusterId(@Param("clusterId") Long clusterId);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster WHERE cluster.id= :clusterId "
            + "AND r.status <> 'DEFAULT_DELETED' AND r.type= :type")
    RDSConfig findByClusterIdAndType(@Param("clusterId") Long clusterId, @Param("type") String type);

    @Override
    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters LEFT JOIN FETCH r.environments e WHERE "
            + "r.workspace.id = :workspaceId AND :environment in e AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByWorkspaceIdAndEnvironments(@Param("workspaceId") Long workspaceId, @Param("environment") EnvironmentView environment);

    @Override
    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters LEFT JOIN FETCH r.environments e WHERE "
            + "r.workspace.id = :workspaceId AND e IS NULL AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByWorkspaceIdAndEnvironmentsIsNull(@Param("workspaceId") Long workspaceId);

    @Override
    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters LEFT JOIN FETCH r.environments e WHERE "
            + "r.workspace.id = :workspaceId AND e IS NOT NULL AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByWorkspaceIdAndEnvironmentsIsNotNull(@Param("workspaceId") Long workspaceId);

    @Override
    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.workspace.id = :workspaceId "
            + "AND r.name in :names AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByNameInAndWorkspaceId(@Param("names") Collection<String> names, @Param("workspaceId") Long workspaceId);
}
