package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = FlexSubscription.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.FLEXSUBSCRIPTION)
public interface FlexSubscriptionRepository extends WorkspaceResourceRepository<FlexSubscription, Long> {

    @CheckPermissionsByWorkspace(action = READ, workspaceIndex = 1)
    Long countByNameAndWorkspace(String name, Workspace workspace);

    @CheckPermissionsByWorkspace(action = READ, workspaceIndex = 1)
    Long countBySubscriptionIdAndWorkspace(String subscriptionId, Workspace workspace);
}
