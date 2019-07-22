package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.repository.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.authorization.resource.AuthorizationResource;

@DisableHasPermission
@EntityType(entityClass = Network.class)
@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface TopologyRepository extends WorkspaceResourceRepository<Topology, Long> {

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
