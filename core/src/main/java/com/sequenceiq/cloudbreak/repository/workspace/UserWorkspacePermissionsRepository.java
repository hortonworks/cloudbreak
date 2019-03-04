package com.sequenceiq.cloudbreak.repository.workspace;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = UserWorkspacePermissions.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface UserWorkspacePermissionsRepository extends DisabledBaseRepository<UserWorkspacePermissions, Long> {

    @Query("SELECT o FROM UserWorkspacePermissions o WHERE o.user = :user")
    Set<UserWorkspacePermissions> findForUser(@Param("user") User user);

    @Query("SELECT o FROM UserWorkspacePermissions o WHERE o.workspace = :workspace")
    Set<UserWorkspacePermissions> findForWorkspace(@Param("workspace") Workspace workspace);

    @Query("SELECT o FROM UserWorkspacePermissions o WHERE o.user = :user AND o.workspace = :workspace")
    UserWorkspacePermissions findForUserAndWorkspace(@Param("user") User user, @Param("workspace") Workspace workspace);

    @Query("SELECT o FROM UserWorkspacePermissions o WHERE o.user = :user AND o.workspace.id = :workspaceId")
    UserWorkspacePermissions findForUserByWorkspaceId(@Param("user") User user, @Param("workspaceId") Long workspaceId);

    @Query("SELECT o FROM UserWorkspacePermissions o WHERE o.user = :user AND o.workspace.id in :workspaceIds")
    Set<UserWorkspacePermissions> findForUserByWorkspaceIds(@Param("user") User user, @Param("workspaceIds") Set<Long> workspaceIds);

    Long deleteByWorkspace(Workspace workspace);
}
