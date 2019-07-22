package com.sequenceiq.cloudbreak.repository.cluster;


import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@DisableHasPermission
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterView.class)
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface ClusterViewRepository extends WorkspaceResourceRepository<ClusterView, Long> {


}
