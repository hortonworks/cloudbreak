package com.sequenceiq.cloudbreak.workspace.repository.workspace;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Tenant.class)
@Transactional(TxType.REQUIRED)
public interface TenantRepository extends CrudRepository<Tenant, Long> {

    Optional<Tenant> findByName(String name);
}
