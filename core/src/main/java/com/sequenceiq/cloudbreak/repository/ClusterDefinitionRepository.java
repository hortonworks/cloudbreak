package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ClusterDefinition.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.CLUSTER_DEFINITION)
public interface ClusterDefinitionRepository extends WorkspaceResourceRepository<ClusterDefinition, Long> {

    @Query("SELECT b FROM ClusterDefinition b WHERE b.workspace.id= :workspaceId AND b.status <> 'DEFAULT_DELETED'")
    @CheckPermissionsByReturnValue
    Set<ClusterDefinition> findAllByNotDeletedInWorkspace(@Param("workspaceId") Long workspaceId);

    @CheckPermissionsByReturnValue
    Set<ClusterDefinition> findAllByWorkspaceIdAndStatus(Long workspaceId, ResourceStatus status);

    @Override
    @DisableHasPermission
    @CheckPermissionsByTarget(action = READ, targetIndex = 0)
    <S extends ClusterDefinition> Iterable<S> saveAll(Iterable<S> entities);
}