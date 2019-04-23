package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseReadonlyRepository;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.BlueprintArchived;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = BlueprintArchived.class)
@Transactional(Transactional.TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.BLUEPRINT)
public interface BlueprintArchivedRepository extends DisabledBaseReadonlyRepository<BlueprintArchived, Long> {

    @CheckPermissionsByReturnValue
    Set<BlueprintArchived> findAllByWorkspaceIdAndStatus(Long workspaceId, ResourceStatus status);
}
