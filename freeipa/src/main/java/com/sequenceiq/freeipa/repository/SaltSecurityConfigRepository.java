package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;

@EntityType(entityClass = SaltSecurityConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface SaltSecurityConfigRepository extends DisabledBaseRepository<SaltSecurityConfig, Long> {
}
