package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceStatus;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public class WorkspaceConfiguratorFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceConfiguratorFilter.class);

    private final CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    private final WorkspaceService workspaceService;

    private final UserService userService;

    private final AuthenticatedUserService authenticatedUserService;

    public WorkspaceConfiguratorFilter(CloudbreakRestRequestThreadLocalService restRequestThreadLocalService, WorkspaceService workspaceService,
            UserService userService, AuthenticatedUserService authenticatedUserService) {
        this.restRequestThreadLocalService = restRequestThreadLocalService;
        this.workspaceService = workspaceService;
        this.userService = userService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        if (cloudbreakUser != null) {
            User user = userService.getOrCreate(cloudbreakUser);
            String workspaceName = cloudbreakUser.getUserCrn() != null ? Crn.fromString(cloudbreakUser.getUserCrn()).getAccountId()
                    : cloudbreakUser.getTenant();
            Optional<Workspace> tenantDefaultWorkspace = workspaceService.getByName(workspaceName, user);
            if (!tenantDefaultWorkspace.isPresent()) {
                tenantDefaultWorkspace = createTenantDefaultWorkspace(user, workspaceName);
            }
            Long workspaceId = tenantDefaultWorkspace.get().getId();
            restRequestThreadLocalService.setRequestedWorkspaceId(workspaceId);
            filterChain.doFilter(new WorkspaceIdModifiedRequest(request, workspaceId), response);
        } else {
            filterChain.doFilter(request, response);
        }
        restRequestThreadLocalService.removeRequestedWorkspaceId();
    }

    private Optional<Workspace> createTenantDefaultWorkspace(User user, String workspaceName) {
        Workspace workspace = new Workspace();
        workspace.setTenant(user.getTenant());
        workspace.setName(workspaceName);
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setResourceCrnByUser(user);
        return Optional.of(workspaceService.create(workspace));
    }
}
