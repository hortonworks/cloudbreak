package com.sequenceiq.cloudbreak.repository.cluster;


import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterView.class)
public interface ClusterViewRepository extends WorkspaceResourceRepository<ClusterView, Long> {


}
