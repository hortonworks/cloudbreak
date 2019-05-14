package com.sequenceiq.environment.environment.repository.network;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.environment.environment.domain.network.AwsNetwork;

@DisableHasPermission
@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = AwsNetwork.class)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface AwsNetworkRepository extends BaseNetworkRepository<AwsNetwork> {
}
