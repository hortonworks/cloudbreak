package com.sequenceiq.cloudbreak.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query("UPDATE Network n SET n.networkCidrs = :networkCidrs "
            + "WHERE n.id IN (SELECT s.network.id FROM Stack s WHERE s.environmentCrn = :environmentCrn AND s.terminated IS NULL)")
    int updateNetworkCidrsByEnvironmentCrn(@Param("environmentCrn") String environmentCrn, @Param("networkCidrs") String networkCidrs);
}
