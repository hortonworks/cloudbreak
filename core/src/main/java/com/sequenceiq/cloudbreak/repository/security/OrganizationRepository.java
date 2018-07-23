package com.sequenceiq.cloudbreak.repository.security;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.Tenant;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Organization.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface OrganizationRepository extends DisabledBaseRepository<Organization, Long> {

    @Query("SELECT o FROM Organization o WHERE o.name= :name AND o.tenant= :tenant")
    Organization getByName(@Param("name") String name, @Param("tenant") Tenant tenant);

}
