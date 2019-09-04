package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = ClusterTemplate.class)
@Transactional(TxType.REQUIRED)
public interface ClusterTemplateRepository extends WorkspaceResourceRepository<ClusterTemplate, Long> {

    @Override
    <S extends ClusterTemplate> Iterable<S> saveAll(Iterable<S> entities);

    @Query("SELECT c FROM ClusterTemplate c WHERE c.workspace.id= :workspaceId AND c.status <> 'DEFAULT_DELETED'")
    Set<ClusterTemplate> findAllByNotDeletedInWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("SELECT c FROM ClusterTemplate c WHERE c.resourceCrn= :crn AND c.workspace.id= :workspaceId AND c.status <> 'DEFAULT_DELETED'")
    Optional<ClusterTemplate> getByCrnForWorkspaceId(@Param("crn") String crn, @Param("workspaceId") Long workspaceId);

    @Query("SELECT c FROM ClusterTemplate c WHERE c.stackTemplate.environmentCrn= :environmentCrn AND c.status <> 'DEFAULT_DELETED'")
    Set<ClusterTemplate> getAllByEnvironmentCrn(@Param("environmentCrn") String environmentCrn);

}
