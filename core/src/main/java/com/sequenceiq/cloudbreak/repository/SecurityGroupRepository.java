package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.repository.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@DisableHasPermission
@EntityType(entityClass = Network.class)
@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface SecurityGroupRepository extends WorkspaceResourceRepository<SecurityGroup, Long> {

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
    Optional<SecurityGroup> findByNameAndWorkspace(String name, Workspace workspace);
}
