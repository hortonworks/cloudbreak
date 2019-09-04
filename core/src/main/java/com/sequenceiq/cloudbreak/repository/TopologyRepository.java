package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = Network.class)
@Transactional(TxType.REQUIRED)
public interface TopologyRepository extends WorkspaceResourceRepository<Topology, Long> {

    @Override
    Topology save(Topology entity);

    @Override
    void delete(Topology entity);

    @Override
    Optional<Topology> findById(Long id);

}
