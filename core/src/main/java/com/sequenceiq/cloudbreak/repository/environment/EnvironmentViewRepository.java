package com.sequenceiq.cloudbreak.repository.environment;

import static com.sequenceiq.cloudbreak.authorization.ResourceAction.READ;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
