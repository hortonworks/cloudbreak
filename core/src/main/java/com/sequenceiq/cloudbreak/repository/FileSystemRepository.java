package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.domain.FileSystem;

@EntityType(entityClass = FileSystem.class)
@Transactional(TxType.REQUIRED)
@HasPermission
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface FileSystemRepository extends WorkspaceResourceRepository<FileSystem, Long> {

}
