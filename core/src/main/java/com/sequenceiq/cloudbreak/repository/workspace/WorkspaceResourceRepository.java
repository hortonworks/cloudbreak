package com.sequenceiq.cloudbreak.repository.workspace;

import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface WorkspaceResourceRepository<T extends WorkspaceAwareResource, ID extends Serializable> extends DisabledBaseRepository<T, ID> {

    @CheckPermissionsByWorkspace(action = READ, workspaceIndex = 0)
    Set<T> findAllByWorkspace(Workspace workspace);

    @CheckPermissionsByWorkspaceId(action = READ)
    Set<T> findAllByWorkspaceId(Long workspaceId);

    @CheckPermissionsByWorkspace(action = READ, workspaceIndex = 1)
    Optional<T> findByNameAndWorkspace(String name, Workspace workspace);

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Optional<T> findByNameAndWorkspaceId(String name, Long workspaceId);

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Set<T> findByNameInAndWorkspaceId(Set<String> names, Long workspaceId);
}
