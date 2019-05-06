package com.sequenceiq.cloudbreak.workspace.repository.workspace;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface WorkspaceResourceRepository<T extends WorkspaceAwareResource, ID extends Serializable> extends DisabledBaseRepository<T, ID> {

    @CheckPermissionsByWorkspace(action = ResourceAction.READ, workspaceIndex = 0)
    Set<T> findAllByWorkspace(Workspace workspace);

    @CheckPermissionsByWorkspaceId(action = ResourceAction.READ)
    Set<T> findAllByWorkspaceId(Long workspaceId);

    @CheckPermissionsByWorkspace(action = ResourceAction.READ, workspaceIndex = 1)
    Optional<T> findByNameAndWorkspace(String name, Workspace workspace);

    @CheckPermissionsByWorkspaceId(action = ResourceAction.READ, workspaceIdIndex = 1)
    Optional<T> findByNameAndWorkspaceId(String name, Long workspaceId);

    @CheckPermissionsByWorkspaceId(action = ResourceAction.READ, workspaceIdIndex = 1)
    Set<T> findByNameInAndWorkspaceId(Set<String> names, Long workspaceId);
}
