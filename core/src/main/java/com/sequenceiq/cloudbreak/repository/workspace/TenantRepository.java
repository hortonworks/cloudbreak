package com.sequenceiq.cloudbreak.repository.workspace;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Tenant.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface TenantRepository extends DisabledBaseRepository<Tenant, Long> {

    Tenant findByName(String name);
}
