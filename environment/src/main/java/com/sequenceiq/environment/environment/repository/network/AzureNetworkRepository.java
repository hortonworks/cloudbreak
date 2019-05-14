package com.sequenceiq.environment.environment.repository.network;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.environment.environment.domain.network.AzureNetwork;

@DisableHasPermission
@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = AzureNetwork.class)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface AzureNetworkRepository extends BaseNetworkRepository<AzureNetwork> {
}
