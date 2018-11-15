package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public class WorkspaceConfiguratorFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceConfiguratorFilter.class);

    private final Pattern v3ResourcePattern = Pattern.compile(".*\\/v3\\/(\\d*)\\/.*");

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
        String requestURI = request.getRequestURI();
        Matcher v3ResourceMatcher = v3ResourcePattern.matcher(requestURI);

        if (v3ResourceMatcher.matches()) {
            String workspaceIdString = v3ResourceMatcher.group(1);
            try {
                Long workspaceId = Long.valueOf(workspaceIdString);
                restRequestThreadLocalService.setRequestedWorkspaceId(workspaceId);
            } catch (NumberFormatException e) {
                LOGGER.error(String.format("WorkspaceID couldn't be parsed from the V3 request URI: %s", requestURI), e);
            }
        } else {
            CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
            if (cloudbreakUser != null) {
                User user = userService.getOrCreate(cloudbreakUser);
                Workspace workspace = workspaceService.getDefaultWorkspaceForUser(user);
                if (workspace == null) {
                    throw new NotFoundException("Workspace not found");
                }
                restRequestThreadLocalService.setRequestedWorkspaceId(workspace.getId());
            }
        }
        filterChain.doFilter(request, response);
        restRequestThreadLocalService.removeRequestedWorkspaceId();
    }
}
