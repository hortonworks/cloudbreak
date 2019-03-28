package com.sequenceiq.cloudbreak.repository.environment;

import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.NoRepositoryBean;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;

@NoRepositoryBean
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface EnvironmentResourceRepository<T extends EnvironmentAwareResource, ID extends Serializable> extends WorkspaceResourceRepository<T, ID> {

    @CheckPermissionsByWorkspaceId(action = READ)
    Set<T> findAllByWorkspaceIdAndEnvironments(Long workspaceId, EnvironmentView environment);

    @CheckPermissionsByWorkspaceId(action = READ)
    Set<T> findAllByWorkspaceIdAndEnvironmentsIsNull(Long workspaceId);

    @CheckPermissionsByWorkspaceId(action = READ)
    Set<T> findAllByWorkspaceIdAndEnvironmentsIsNotNull(Long workspaceId);

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Set<T> findAllByNameInAndWorkspaceId(Collection<String> names, Long workspaceId);
}
