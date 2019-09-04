package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = Network.class)
@Transactional(TxType.REQUIRED)
public interface SecurityGroupRepository extends WorkspaceResourceRepository<SecurityGroup, Long> {

    @Override
    SecurityGroup save(SecurityGroup entity);

    @Override
    void delete(SecurityGroup entity);

    @Override
    Optional<SecurityGroup> findById(Long id);

    @Override
    Optional<SecurityGroup> findByNameAndWorkspace(String name, Workspace workspace);
}
