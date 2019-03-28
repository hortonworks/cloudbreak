package com.sequenceiq.cloudbreak.repository.cluster;

import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

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
