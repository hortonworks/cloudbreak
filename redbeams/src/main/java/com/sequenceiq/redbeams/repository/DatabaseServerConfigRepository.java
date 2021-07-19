package com.sequenceiq.redbeams.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.RedbeamsResourceCrnAndNameView;

@EntityType(entityClass = DatabaseServerConfig.class)
@Transactional(TxType.REQUIRED)
public interface DatabaseServerConfigRepository extends JpaRepository<DatabaseServerConfig, Long> {

    Set<DatabaseServerConfig> findByWorkspaceIdAndEnvironmentId(Long workspaceId, String environmentId);

    Optional<DatabaseServerConfig> findByResourceCrn(Crn crn);

    Optional<DatabaseServerConfig> findByNameAndWorkspaceIdAndEnvironmentId(String name, Long workspaceId, String environmentId);

    Set<DatabaseServerConfig> findByResourceCrnIn(Set<Crn> resourceCrns);

    Optional<DatabaseServerConfig> findByName(String name);

    @Query("SELECT c.resourceCrn FROM DatabaseServerConfig c WHERE c.name = :name")
    Optional<Crn> findResourceCrnByName(@Param("name") String name);

    List<RedbeamsResourceCrnAndNameView> findByResourceCrnIn(Collection<Crn> resourceCrns);

    @Query("SELECT c.resourceCrn FROM DatabaseServerConfig c WHERE c.name IN (:names)")
    List<Crn> findResourceCrnsByNames(@Param("names") Collection<String> names);

    Optional<DatabaseServerConfig> findByEnvironmentIdAndClusterCrn(String environmentId, String clusterCrn);
}
