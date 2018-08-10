package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = LdapConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.LDAP)
public interface LdapConfigRepository extends OrganizationResourceRepository<LdapConfig, Long> {

}
