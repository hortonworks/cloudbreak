package com.sequenceiq.cloudbreak.repository.organization;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.Tenant;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Organization.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface OrganizationRepository extends DisabledBaseRepository<Organization, Long> {

    @Query("SELECT o FROM Organization o WHERE o.name= :name AND o.tenant= :tenant AND o.status <> 'DELETED'")
    Organization getByName(@Param("name") String name, @Param("tenant") Tenant tenant);

    @Override
    @Query("SELECT o FROM Organization o WHERE o.id= :id AND o.status <> 'DELETED'")
    Optional<Organization> findById(@Param("id") Long id);

    @Override
    @Query("SELECT o FROM Organization o WHERE o.id= :id AND o.status <> 'DELETED'")
    boolean existsById(@Param("id") Long id);

    @Override
    @Query("SELECT o FROM Organization o WHERE o.status <> 'DELETED'")
    Iterable<Organization> findAll();

    @Override
    @Query("SELECT o FROM Organization o WHERE o.status <> 'DELETED' AND o.id IN :ids")
    Iterable<Organization> findAllById(@Param("ids") Iterable<Long> ids);
}
