package com.sequenceiq.environment.environment;

import static com.sequenceiq.cloudbreak.workspace.resource.ResourceAction.READ;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.workspace.repository.check.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.workspace.repository.check.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@DisableHasPermission
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = EnvironmentView.class)
@WorkspaceResourceType(resource = WorkspaceResource.ENVIRONMENT)
public interface EnvironmentViewRepository extends WorkspaceResourceRepository<EnvironmentView, Long> {

    @CheckPermissionsByWorkspaceId(action = READ)
    @Query("SELECT ev FROM EnvironmentView ev LEFT JOIN FETCH ev.workspace w LEFT JOIN FETCH w.tenant LEFT JOIN FETCH ev.credential "
            + "LEFT JOIN FETCH ev.datalakeResources WHERE w.id= :id")
    Set<EnvironmentView> findAllByWorkspaceId(@Param("id") Long workspaceId);

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Set<EnvironmentView> findAllByNameInAndWorkspaceId(Collection<String> names, Long workspaceId);

    @DisableCheckPermissions
    Set<EnvironmentView> findAllByCredentialId(Long credentialId);

    @CheckPermissionsByWorkspaceId(action = READ, workspaceIdIndex = 1)
    Long getIdByNameAndWorkspaceId(String name, Long workspaceId);
}
