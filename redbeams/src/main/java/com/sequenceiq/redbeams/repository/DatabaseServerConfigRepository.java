package com.sequenceiq.redbeams.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.RedbeamsResourceCrnAndNameView;

@EntityType(entityClass = DatabaseServerConfig.class)
@Transactional(TxType.REQUIRED)
public interface DatabaseServerConfigRepository extends JpaRepository<DatabaseServerConfig, Long>, VaultRotationAwareRepository {

    Set<DatabaseServerConfig> findByWorkspaceIdAndEnvironmentId(Long workspaceId, String environmentId);

    @Query("SELECT d FROM DatabaseServerConfig d WHERE d.environmentId IN (:environmentIds) AND d.workspaceId = :workspaceId")
    Set<DatabaseServerConfig> findByWorkspaceIdAndEnvironmentIds(@Param("workspaceId") Long workspaceId,
        @Param("environmentIds") Collection<String> environmentIds);

    Optional<DatabaseServerConfig> findByResourceCrn(Crn crn);

    Optional<DatabaseServerConfig> findByNameAndWorkspaceIdAndEnvironmentId(String name, Long workspaceId, String environmentId);

    Set<DatabaseServerConfig> findByResourceCrnIn(Set<Crn> resourceCrns);

    @Query("SELECT d FROM DatabaseServerConfig d WHERE d.clusterCrn IN (:clusterCrns) AND d.accountId = :accountId")
    Set<DatabaseServerConfig> findByAccountIdAndClusterCrns(@Param("accountId") String accountId,
        @Param("clusterCrns") Collection<String> clusterCrns);

    @Query("SELECT d FROM DatabaseServerConfig d WHERE d.environmentId IN (:environmentIds) AND d.accountId = :accountId")
    Set<DatabaseServerConfig> findByAccountIdAndEnvironmentIds(@Param("accountId") String accountId,
        @Param("environmentIds") Collection<String> environmentIds);

    Optional<DatabaseServerConfig> findByName(String name);

    @Query("SELECT c.resourceCrn FROM DatabaseServerConfig c WHERE c.name = :name")
    Optional<Crn> findResourceCrnByName(@Param("name") String name);

    List<RedbeamsResourceCrnAndNameView> findByResourceCrnIn(Collection<Crn> resourceCrns);

    @Query("SELECT c.resourceCrn FROM DatabaseServerConfig c WHERE c.name IN (:names)")
    List<Crn> findResourceCrnsByNames(@Param("names") Collection<String> names);

    List<DatabaseServerConfig> findByEnvironmentIdAndClusterCrn(String environmentId, String clusterCrn);

    @Override
    default Class<DatabaseServerConfig> getEntityClass() {
        return DatabaseServerConfig.class;
    }
}
