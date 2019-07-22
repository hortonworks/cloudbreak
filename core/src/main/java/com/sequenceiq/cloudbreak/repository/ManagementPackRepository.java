package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = ManagementPack.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface ManagementPackRepository extends WorkspaceResourceRepository<ManagementPack, Long> {

}
