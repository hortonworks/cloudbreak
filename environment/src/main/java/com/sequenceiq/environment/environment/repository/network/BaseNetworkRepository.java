package com.sequenceiq.environment.environment.repository.network;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;

@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = BaseNetwork.class)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface BaseNetworkRepository<T extends BaseNetwork> extends WorkspaceResourceRepository<T, Long> {
}
