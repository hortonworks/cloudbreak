package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@EntityType(entityClass = Gateway.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface GatewayRepository extends JpaRepository<Gateway, Long> {
}
