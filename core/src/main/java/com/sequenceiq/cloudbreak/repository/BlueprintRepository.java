package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = Blueprint.class)
@Transactional(TxType.REQUIRED)
public interface BlueprintRepository extends WorkspaceResourceRepository<Blueprint, Long> {

    @Query("SELECT b FROM Blueprint b WHERE b.workspace.id= :workspaceId AND b.status <> 'DEFAULT_DELETED'")
    Set<Blueprint> findAllByNotDeletedInWorkspace(@Param("workspaceId") Long workspaceId);

    Set<Blueprint> findAllByWorkspaceIdAndStatusIn(Long workspaceId, Set<ResourceStatus> statuses);

    @Override
    <S extends Blueprint> Iterable<S> saveAll(Iterable<S> entities);

    Optional<Blueprint> findByResourceCrnAndWorkspaceId(String resourceCrn, Long workspaceId);

}