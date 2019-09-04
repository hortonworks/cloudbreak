package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Gateway.class)
@Transactional(TxType.REQUIRED)
public interface GatewayRepository extends CrudRepository<Gateway, Long> {
}
