package com.sequenceiq.cloudbreak.repository.cluster;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = DatalakeResources.class)
@Transactional(Transactional.TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.DATALAKE_RESOURCES)
public interface DatalakeResourcesRepository extends WorkspaceResourceRepository<DatalakeResources, Long> {
    @CheckPermissionsByReturnValue
    DatalakeResources findByDatalakeStackId(Long datalakeStackId);
}
