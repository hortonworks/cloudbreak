package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Gateway.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface GatewayRepository extends DisabledBaseRepository<Gateway, Long> {
}
