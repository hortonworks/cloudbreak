package com.sequenceiq.environment.environment.repository;

import static com.sequenceiq.cloudbreak.workspace.resource.ResourceAction.READ;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.environment.environment.domain.Environment;

@DisableHasPermission
@EntityType(entityClass = Environment.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface EnvironmentRepository extends WorkspaceResourceRepository<Environment, Long> {

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Set<Environment> findAllByNameInAndWorkspaceId(Set<String> names, Long workspaceId);
}
