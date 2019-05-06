package com.sequenceiq.cloudbreak.repository.environment;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.environment.AwsNetwork;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@DisableHasPermission
@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = AwsNetwork.class)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface AwsNetworkRepository extends BaseNetworkRepository<AwsNetwork> {
}
