package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.STACK;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.autoscale.AutoscaleUserAuthorizationEndpoint;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(Transactional.TxType.NEVER)
public class AutoscaleUserAuthorizationController implements AutoscaleUserAuthorizationEndpoint {

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackService stackService;

    @Inject
    private UserService userService;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public Boolean authorizeForAutoscale(Long id, String owner, String permission) {
        try {
            restRequestThreadLocalService.setCloudbreakUserByOwner(owner);
            Stack stack = stackService.get(id);
            if (WorkspacePermissions.Action.WRITE.name().equalsIgnoreCase(permission)) {
                User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
                permissionCheckingUtils.checkPermissionByWorkspaceIdForUser(stack.getWorkspace().getId(), STACK, WorkspacePermissions.Action.WRITE, user);
            }
            return true;
        } catch (RuntimeException ignore) {
            return false;
        }
    }
}
