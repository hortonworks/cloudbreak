package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@EntityType(entityClass = Gateway.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface GatewayRepository extends DisabledBaseRepository<Gateway, Long> {
}
