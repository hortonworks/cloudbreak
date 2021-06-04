package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Network.class)
@Transactional(TxType.REQUIRED)
public interface InstanceGroupNetworkRepository extends CrudRepository<InstanceGroupNetwork, Long> {

    @Override
    InstanceGroupNetwork save(InstanceGroupNetwork entity);

    @Override
    void delete(InstanceGroupNetwork entity);

    @Override
    Optional<InstanceGroupNetwork> findById(Long id);
}
