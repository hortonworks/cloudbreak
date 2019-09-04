package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = Network.class)
@Transactional(TxType.REQUIRED)
public interface NetworkRepository extends WorkspaceResourceRepository<Network, Long> {

    @Override
    Network save(Network entity);

    @Override
    void delete(Network entity);
}
