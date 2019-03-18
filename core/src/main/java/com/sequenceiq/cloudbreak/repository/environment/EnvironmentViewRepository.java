package com.sequenceiq.cloudbreak.repository.environment;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = EnvironmentView.class)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface EnvironmentViewRepository extends WorkspaceResourceRepository<EnvironmentView, Long> {

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Set<EnvironmentView> findAllByNameInAndWorkspaceIdAndArchivedFalse(Collection<String> names, Long workspaceId);

    @DisableCheckPermissions
    Set<EnvironmentView> findAllByCredentialIdAndArchivedFalse(Long credentialId);

    @CheckPermissionsByWorkspaceId(action = READ)
    Set<EnvironmentView> findAllByWorkspaceIdAndArchivedFalse(Long workspaceid);

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    EnvironmentView getByNameAndWorkspaceIdAndArchivedFalse(String name, Long workspaceid);
}
