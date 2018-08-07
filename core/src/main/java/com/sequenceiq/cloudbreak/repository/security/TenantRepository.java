package com.sequenceiq.cloudbreak.repository.security;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.security.Tenant;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Tenant.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface TenantRepository extends DisabledBaseRepository<Tenant, Long> {

    Tenant findByName(String name);
}
