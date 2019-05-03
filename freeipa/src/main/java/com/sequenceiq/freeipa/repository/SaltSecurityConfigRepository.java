package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;

@EntityType(entityClass = SaltSecurityConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface SaltSecurityConfigRepository extends DisabledBaseRepository<SaltSecurityConfig, Long> {
}
