package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Network.class)
@Transactional(Transactional.TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.TOPOLOGY)
public interface TopologyRepository extends OrganizationResourceRepository<Topology, Long> {

    @Override
    @DisableCheckPermissions
    Topology save(Topology entity);

    @Override
    @DisableCheckPermissions
    void delete(Topology entity);

    @Override
    @DisableCheckPermissions
    Optional<Topology> findById(Long id);
}
