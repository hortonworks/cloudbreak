package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Network.class)
@Transactional(Transactional.TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.NETWORK)
public interface NetworkRepository extends OrganizationResourceRepository<Network, Long> {

    @Override
    @DisableCheckPermissions
    Network save(Network entity);

    @Override
    @DisableCheckPermissions
    void delete(Network entity);
}
