package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ProxyConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@OrganizationResourceType(resource = OrganizationResource.PROXY)
public interface ProxyConfigRepository extends OrganizationResourceRepository<ProxyConfig, Long> {

    @Override
    @CheckPermissionsByOrganizationId
    @Query("SELECT p FROM ProxyConfig p WHERE p.organization.id = :orgId")
    Set<ProxyConfig> findAllByOrganizationId(@Param("orgId") Long orgId);
}
