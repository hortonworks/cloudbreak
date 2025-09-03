package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = RDSConfig.class)
@Transactional(TxType.REQUIRED)
public interface RdsConfigRepository extends WorkspaceResourceRepository<RDSConfig, Long>, VaultRotationAwareRepository {

    @Override
    @Query("SELECT r FROM RDSConfig r WHERE r.workspace.id = :workspaceId AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Override
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.workspace = :org AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByWorkspace(@Param("org") Workspace org);

    @Override
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name AND r.status = 'USER_MANAGED'"
            + "AND r.workspace.id = :workspaceId")
    Optional<RDSConfig> findByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

    @Override
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name in :names AND r.status = 'USER_MANAGED'"
            + "AND r.workspace.id = :workspaceId")
    Set<RDSConfig> findByNameInAndWorkspaceId(@Param("names") Set<String> name, @Param("workspaceId") Long workspaceId);

    @Override
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name AND r.status = 'USER_MANAGED'"
            + "AND r.workspace = :org")
    Optional<RDSConfig> findByNameAndWorkspace(@Param("name") String name, @Param("org") Workspace org);

    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster LEFT JOIN FETCH r.clusters WHERE cluster.id= :clusterId")
    Set<RDSConfig> findByClusterId(@Param("clusterId") Long clusterId);

    @Query("SELECT count(r) > 0 FROM RDSConfig r INNER JOIN r.clusters cluster WHERE cluster.id= :clusterId "
            + "AND r.status <> 'DEFAULT_DELETED' AND r.type= :type")
    Boolean existsByClusterIdAndType(@Param("clusterId") Long clusterId, @Param("type") String type);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.workspace.id = :workspaceId "
            + "AND r.name in :names AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findAllByNameInAndWorkspaceId(@Param("names") Collection<String> names, @Param("workspaceId") Long workspaceId);

    @Query("SELECT rc FROM Stack s LEFT JOIN s.cluster c LEFT JOIN c.rdsConfigs rc " +
            "WHERE s.resourceCrn = :stackCrn AND rc.type = :databaseType AND rc.status = 'DEFAULT'")
    Optional<RDSConfig> findByStackIdAndType(@Param("stackCrn") String stackCrn, @Param("databaseType") String databaseType);

    @Query("SELECT r FROM RDSConfig r WHERE r.connectionURL = :connectionUrl")
    Set<RDSConfig> findAllByConnectionUrlAndType(@Param("connectionUrl") String connectionUrl);

    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.connectionURL = :connectionUrl")
    Set<RDSConfig> findAllByConnectionUrlAndTypeWithClusters(@Param("connectionUrl") String connectionUrl);

    @Modifying
    @Query("UPDATE RDSConfig r SET r.sslMode = 'ENABLED' WHERE r.id = :id")
    void enableSsl(@Param("id") Long id);

    @Override
    default Class<RDSConfig> getEntityClass() {
        return RDSConfig.class;
    }
}
