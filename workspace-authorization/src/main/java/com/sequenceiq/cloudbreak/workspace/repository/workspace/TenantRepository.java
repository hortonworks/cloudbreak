package com.sequenceiq.cloudbreak.workspace.repository.workspace;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Tenant.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface TenantRepository extends DisabledBaseRepository<Tenant, Long> {

    Optional<Tenant> findByName(String name);
}
