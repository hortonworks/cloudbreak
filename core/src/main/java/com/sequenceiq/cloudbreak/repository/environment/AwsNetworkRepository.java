package com.sequenceiq.cloudbreak.repository.environment;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.environment.AwsNetwork;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = AwsNetwork.class)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface AwsNetworkRepository extends BaseNetworkRepository<AwsNetwork> {
}
