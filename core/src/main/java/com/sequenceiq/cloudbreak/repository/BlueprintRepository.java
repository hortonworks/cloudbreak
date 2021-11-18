package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.projection.BlueprintStatusView;
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

    Blueprint findByResourceCrn(String resourceCrn);

    @Query("SELECT b.resourceCrn FROM Blueprint b WHERE b.name = :name AND b.workspace.tenant.name = :accountId")
    Optional<String> findResourceCrnByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT b.name as name, b.resourceCrn as crn FROM Blueprint b WHERE b.workspace.tenant.name = :accountId AND b.resourceCrn IN (:resourceCrns)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndAccountId(@Param("resourceCrns") Collection<String> resourceCrns,
            @Param("accountId") String accountId);

    @Query("SELECT b.name FROM Blueprint b WHERE b.resourceCrn = :resourceCrn AND b.workspace.tenant.name = :accountId")
    Optional<String> findResourceNameByCrnAndAccountId(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT b.resourceCrn FROM Blueprint b WHERE b.workspace.tenant.name = :accountId")
    List<String> findAllResourceCrnsByAccountId(@Param("accountId") String accountId);

    @Query("SELECT b.status as status FROM Blueprint b WHERE b.resourceCrn = :resourceCrn")
    BlueprintStatusView findViewByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT s.cluster.blueprint FROM Stack s WHERE s.resourceCrn = :datahubCrn AND s.type = 'WORKLOAD'")
    Optional<Blueprint> findByDatahubCrn(@Param("datahubCrn") String datahubCrn);
}