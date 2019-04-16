package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Network.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.STACK)
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
