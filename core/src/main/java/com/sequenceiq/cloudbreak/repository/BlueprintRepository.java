package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.authorization.resource.ResourceAction.READ;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTarget;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@EntityType(entityClass = Blueprint.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface BlueprintRepository extends WorkspaceResourceRepository<Blueprint, Long> {

    @Query("SELECT b FROM Blueprint b WHERE b.workspace.id= :workspaceId AND b.status <> 'DEFAULT_DELETED'")
    @CheckPermissionsByReturnValue
    Set<Blueprint> findAllByNotDeletedInWorkspace(@Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    Set<Blueprint> findAllByWorkspaceIdAndStatusIn(Long workspaceId, Set<ResourceStatus> statuses);

    @Override
    @DisableHasPermission
    @CheckPermissionsByTarget(action = READ, targetIndex = 0)
    <S extends Blueprint> Iterable<S> saveAll(Iterable<S> entities);

    @CheckPermissionsByReturnValue
    Optional<Blueprint> findByResourceCrnAndWorkspaceId(String resourceCrn, Long workspaceId);

}