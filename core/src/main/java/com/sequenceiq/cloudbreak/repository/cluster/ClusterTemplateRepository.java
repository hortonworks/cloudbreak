package com.sequenceiq.cloudbreak.repository.cluster;

import static com.sequenceiq.cloudbreak.workspace.resource.ResourceAction.READ;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;

@EntityType(entityClass = ClusterTemplate.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.CLUSTER_TEMPLATE)
public interface ClusterTemplateRepository extends WorkspaceResourceRepository<ClusterTemplate, Long> {

    @Override
    @DisableHasPermission
    @CheckPermissionsByTarget(action = READ, targetIndex = 0)
    <S extends ClusterTemplate> Iterable<S> saveAll(Iterable<S> entities);

    @Query("SELECT c FROM ClusterTemplate c WHERE c.workspace.id= :workspaceId AND c.status <> 'DEFAULT_DELETED'")
    @CheckPermissionsByReturnValue
    Set<ClusterTemplate> findAllByNotDeletedInWorkspace(@Param("workspaceId") Long workspaceId);
}
