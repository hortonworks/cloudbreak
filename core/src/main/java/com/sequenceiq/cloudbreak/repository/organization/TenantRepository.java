package com.sequenceiq.cloudbreak.repository.organization;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.security.Tenant;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Tenant.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface TenantRepository extends DisabledBaseRepository<Tenant, Long> {

    Tenant findByName(String name);
}
