package com.sequenceiq.cloudbreak.repository.environment;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = BaseNetwork.class)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface BaseNetworkRepository<T extends BaseNetwork> extends WorkspaceResourceRepository<T, Long> {
}
