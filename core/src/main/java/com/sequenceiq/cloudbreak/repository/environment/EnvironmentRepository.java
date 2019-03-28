package com.sequenceiq.cloudbreak.repository.environment;

import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Environment.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface EnvironmentRepository extends WorkspaceResourceRepository<Environment, Long> {

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Set<Environment> findAllByNameInAndWorkspaceId(Set<String> names, Long workspaceId);
}
