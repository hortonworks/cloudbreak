package com.sequenceiq.cloudbreak.repository.environment;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = EnvironmentView.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface EnvironmentViewRepository extends WorkspaceResourceRepository<EnvironmentView, Long> {

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Set<EnvironmentView> findAllByNameInAndWorkspaceId(Collection<String> names, Long workspaceId);
}
