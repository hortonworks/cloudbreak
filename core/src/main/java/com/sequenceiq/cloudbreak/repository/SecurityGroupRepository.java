package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Network.class)
@Transactional(Transactional.TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.SECURITY_GROUP)
public interface SecurityGroupRepository extends OrganizationResourceRepository<SecurityGroup, Long> {

    @Override
    @DisableCheckPermissions
    SecurityGroup save(SecurityGroup entity);

    @Override
    @DisableCheckPermissions
    void delete(SecurityGroup entity);

    @Override
    @DisableCheckPermissions
    Optional<SecurityGroup> findById(Long id);

    @Override
    @DisableCheckPermissions
    SecurityGroup findByNameAndOrganization(String name, Organization organization);
}
