package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = KerberosConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface KerberosConfigRepository extends DisabledBaseRepository<KerberosConfig, Long> {
}
