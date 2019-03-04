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
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public class WorkspaceConfiguratorFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceConfiguratorFilter.class);

    private final Pattern v4ResourcePattern = Pattern.compile(".*\\/v4\\/(\\d*)\\/.*");

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
        Matcher v4ResourceMatcher = v4ResourcePattern.matcher(requestURI);

        if (v4ResourceMatcher.matches()) {
            String workspaceIdString = v4ResourceMatcher.group(1);
            try {
                Long workspaceId = Long.valueOf(workspaceIdString);
                restRequestThreadLocalService.setRequestedWorkspaceId(workspaceId);
            } catch (NumberFormatException e) {
                LOGGER.info(String.format("WorkspaceID couldn't be parsed from the V4 request URI: %s", requestURI), e);
            }
        } else {
            CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
            if (cloudbreakUser != null) {
                try {
                    User user = userService.getOrCreate(cloudbreakUser);
                    Workspace workspace = workspaceService.getDefaultWorkspaceForUser(user);
                    if (workspace == null) {
                        throw new NotFoundException("Workspace not found");
                    }
                    restRequestThreadLocalService.setRequestedWorkspaceId(workspace.getId());
                } catch (TransactionRuntimeExecutionException e) {
                    throw e.getOriginalCause();
                }
            }
        }
        filterChain.doFilter(request, response);
        restRequestThreadLocalService.removeRequestedWorkspaceId();
    }
}
