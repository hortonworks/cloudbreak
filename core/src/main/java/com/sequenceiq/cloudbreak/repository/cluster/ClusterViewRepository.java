package com.sequenceiq.cloudbreak.repository.cluster;


import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = ClusterView.class)
public interface ClusterViewRepository extends WorkspaceResourceRepository<ClusterView, Long> {


}
