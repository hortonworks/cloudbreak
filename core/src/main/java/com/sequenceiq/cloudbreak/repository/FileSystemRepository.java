package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.HasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.FileSystem;

@EntityType(entityClass = FileSystem.class)
@Transactional(TxType.REQUIRED)
@HasPermission
@WorkspaceResourceType(resource = WorkspaceResource.STACK)
public interface FileSystemRepository extends WorkspaceResourceRepository<FileSystem, Long> {

}
